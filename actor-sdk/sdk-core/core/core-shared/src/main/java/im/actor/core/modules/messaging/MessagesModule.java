/*
 * Copyright (C) 2015 Actor LLC. <https://actor.im>
 */

package im.actor.core.modules.messaging;

import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.ArrayList;
import java.util.HashMap;

import im.actor.core.api.ApiJsonMessage;
import im.actor.core.api.ApiOutPeer;
import im.actor.core.api.ApiPeer;
import im.actor.core.api.ApiPeerType;
import im.actor.core.api.base.SeqUpdate;
import im.actor.core.api.rpc.RequestArchiveChat;
import im.actor.core.api.rpc.RequestClearChat;
import im.actor.core.api.rpc.RequestDeleteChat;
import im.actor.core.api.rpc.RequestDeleteMessage;
import im.actor.core.api.rpc.RequestFavouriteDialog;
import im.actor.core.api.rpc.RequestMessageRemoveReaction;
import im.actor.core.api.rpc.RequestMessageSetReaction;
import im.actor.core.api.rpc.RequestUnfavouriteDialog;
import im.actor.core.api.rpc.ResponseDialogsOrder;
import im.actor.core.api.rpc.ResponseLoadArchived;
import im.actor.core.api.rpc.ResponseReactionsResponse;
import im.actor.core.api.rpc.ResponseSeq;
import im.actor.core.api.updates.UpdateChatClear;
import im.actor.core.api.updates.UpdateChatDelete;
import im.actor.core.api.updates.UpdateChatGroupsChanged;
import im.actor.core.api.updates.UpdateMessageContentChanged;
import im.actor.core.api.updates.UpdateReactionsUpdate;
import im.actor.core.entity.Dialog;
import im.actor.core.entity.DialogSpec;
import im.actor.core.entity.Group;
import im.actor.core.entity.Message;
import im.actor.core.entity.Peer;
import im.actor.core.entity.PeerType;
import im.actor.core.entity.User;
import im.actor.core.entity.content.FastThumb;
import im.actor.core.entity.content.JsonContent;
import im.actor.core.entity.content.internal.Sticker;
import im.actor.core.modules.AbsModule;
import im.actor.core.modules.ModuleContext;
import im.actor.core.events.AppVisibleChanged;
import im.actor.core.events.PeerChatClosed;
import im.actor.core.events.PeerChatOpened;
import im.actor.core.modules.messaging.actions.ArchivedDialogsActor;
import im.actor.core.modules.messaging.actions.CursorReaderActor;
import im.actor.core.modules.messaging.actions.CursorReceiverActor;
import im.actor.core.modules.messaging.actions.SenderActor;
import im.actor.core.modules.messaging.conversation.ConversationActor;
import im.actor.core.modules.messaging.conversation.ConversationHistoryActor;
import im.actor.core.modules.messaging.conversation.ConversationInt;
import im.actor.core.modules.messaging.dialogs.DialogsInt;
import im.actor.core.modules.messaging.router.RouterInt;
import im.actor.core.modules.updates.internal.ChangeContent;
import im.actor.core.network.RpcCallback;
import im.actor.core.network.RpcException;
import im.actor.core.network.RpcInternalException;
import im.actor.core.viewmodel.Command;
import im.actor.core.viewmodel.CommandCallback;
import im.actor.core.viewmodel.ConversationVM;
import im.actor.core.viewmodel.DialogGroupsVM;
import im.actor.core.viewmodel.DialogSpecVM;
import im.actor.runtime.Storage;
import im.actor.runtime.actors.ActorCreator;
import im.actor.runtime.actors.ActorRef;
import im.actor.runtime.actors.Props;
import im.actor.runtime.eventbus.BusSubscriber;
import im.actor.runtime.eventbus.Event;
import im.actor.runtime.files.FileSystemReference;
import im.actor.runtime.mvvm.MVVMCollection;
import im.actor.runtime.storage.ListEngine;
import im.actor.runtime.storage.ListStorage;
import im.actor.runtime.storage.SyncKeyValue;
import im.actor.runtime.storage.memory.MemoryListStorage;

import static im.actor.runtime.actors.ActorSystem.system;

public class MessagesModule extends AbsModule implements BusSubscriber {

    private RouterInt router;
    private DialogsInt dialogs;

    private ActorRef archivedDialogsActor;
    private ActorRef plainReadActor;
    private ActorRef plainReceiverActor;
    private ActorRef sendMessageActor;

    private final HashMap<Peer, ListEngine<Message>> conversationEngines = new HashMap<>();
    private final HashMap<Peer, ListEngine<Message>> conversationDocsEngines = new HashMap<>();
    private final HashMap<Peer, ConversationInt> conversationActors = new HashMap<>();
    private final HashMap<Peer, ActorRef> conversationHistoryActors = new HashMap<>();

    private final SyncKeyValue cursorStorage;

    private final MVVMCollection<DialogSpec, DialogSpecVM> dialogDescKeyValue;

    public MessagesModule(final ModuleContext context) {
        super(context);

        this.dialogDescKeyValue = Storage.createKeyValue(STORAGE_DIALOGS_DESC, DialogSpecVM.CREATOR, DialogSpec.CREATOR);
        this.cursorStorage = new SyncKeyValue(Storage.createKeyValue(STORAGE_CURSOR));
    }

    public void run() {

        this.dialogs = new DialogsInt(context());
        this.router = new RouterInt(context());

        this.archivedDialogsActor = system().actorOf("actor/dialogs/archived", new ActorCreator() {
            @Override
            public ArchivedDialogsActor create() {
                return new ArchivedDialogsActor(context());
            }
        });
        this.plainReadActor = system().actorOf("actor/plain/read", "heavy", new ActorCreator() {
            @Override
            public CursorReaderActor create() {
                return new CursorReaderActor(context());
            }
        });
        this.plainReceiverActor = system().actorOf("actor/plain/receive", "heavy", new ActorCreator() {
            @Override
            public CursorReceiverActor create() {
                return new CursorReceiverActor(context());
            }
        });
        this.sendMessageActor = system().actorOf("actor/sender/small", new ActorCreator() {
            @Override
            public SenderActor create() {
                return new SenderActor(context());
            }
        });

        context().getEvents().subscribe(this, PeerChatOpened.EVENT);
        context().getEvents().subscribe(this, PeerChatClosed.EVENT);
        context().getEvents().subscribe(this, AppVisibleChanged.EVENT);
    }

    public DialogsInt getDialogs() {
        return dialogs;
    }

    public RouterInt getRouter() {
        return router;
    }

    public DialogGroupsVM getDialogGroupsVM() {
        return dialogs.getDialogGroups();
    }

    public MVVMCollection<DialogSpec, DialogSpecVM> getDialogDescKeyValue() {
        return dialogDescKeyValue;
    }

    public ActorRef getSendMessageActor() {
        return sendMessageActor;
    }

    public ActorRef getPlainReadActor() {
        return plainReadActor;
    }

    public ActorRef getPlainReceiverActor() {
        return plainReceiverActor;
    }

    public SyncKeyValue getCursorStorage() {
        return cursorStorage;
    }

    private void assumeConvActor(final Peer peer) {
        synchronized (conversationActors) {
            if (!conversationActors.containsKey(peer)) {
                conversationActors.put(peer, new ConversationInt(system().actorOf(Props.create(new ActorCreator() {
                    @Override
                    public ConversationActor create() {
                        return new ConversationActor(peer, context());
                    }
                }), "actor/conv_" + peer.getPeerType() + "_" + peer.getPeerId())));
                conversationHistoryActors.put(peer, system().actorOf(Props.create(new ActorCreator() {
                    @Override
                    public ConversationHistoryActor create() {
                        return new ConversationHistoryActor(peer, context());
                    }
                }), "actor/conv_" + peer.getPeerType() + "_" + peer.getPeerId() + "/history"));
            }
        }
        if (peer.getPeerType() == PeerType.PRIVATE) {
            context().getEncryption().getEncryptedChatManager(peer.getPeerId());
        }
    }

    public ActorRef getConversationHistoryActor(final Peer peer) {
        assumeConvActor(peer);
        synchronized (conversationActors) {
            return conversationHistoryActors.get(peer);
        }
    }

    public ConversationInt getConversationActor(final Peer peer) {
        assumeConvActor(peer);
        synchronized (conversationActors) {
            return conversationActors.get(peer);
        }
    }

    public ListEngine<Message> getConversationEngine(Peer peer) {
        synchronized (conversationEngines) {
            if (!conversationEngines.containsKey(peer)) {
                String listName = STORAGE_CHAT_PREFIX + peer.getUnuqueId();
                ListStorage storage;
                if (isPersistentEnabled()) {
                    storage = Storage.createList(listName);
                } else {
                    storage = new MemoryListStorage();
                }
                conversationEngines.put(peer, Storage.createList(storage, Message.CREATOR));

            }
            return conversationEngines.get(peer);
        }
    }

    public ListEngine<Message> getConversationDocsEngine(Peer peer) {
        synchronized (conversationDocsEngines) {
            if (!conversationDocsEngines.containsKey(peer)) {
                conversationDocsEngines.put(peer,
                        Storage.createList(STORAGE_CHAT_DOCS_PREFIX + peer.getUnuqueId(), Message.CREATOR));
            }
            return conversationDocsEngines.get(peer);
        }
    }


    public ActorRef getArchivedDialogsActor() {
        return archivedDialogsActor;
    }

    public ListEngine<Dialog> getDialogsEngine() {
        return dialogs.getDialogs();
    }

    public void deleteMessages(Peer peer, long[] rids) {

        ArrayList<Long> deleted = new ArrayList<>();
        for (long rid : rids) {
            deleted.add(rid);
        }
        getRouter().onDeletedMessages(peer, deleted);

        context().getApiModule().performPersistRequest(new RequestDeleteMessage(buildApiOutPeer(peer), deleted));
    }

    public void loadMoreArchivedDialogs(final boolean init, final RpcCallback<ResponseLoadArchived> callback) {
        im.actor.runtime.Runtime.dispatch(new Runnable() {
            @Override
            public void run() {
                archivedDialogsActor.send(new ArchivedDialogsActor.LoadMore(init, callback));
            }
        });
    }

    public void loadMoreHistory(final Peer peer) {
        im.actor.runtime.Runtime.dispatch(new Runnable() {
            @Override
            public void run() {
                getConversationHistoryActor(peer).send(new ConversationHistoryActor.LoadMore());
            }
        });
    }

    public void sendMessage(@NotNull Peer peer, @NotNull String message, @Nullable String markDownText,
                            @Nullable ArrayList<Integer> mentions, boolean autoDetect) {
        // Notify typing about message sent
        context().getTypingModule().onMessageSent(peer);
        sendMessageActor.send(new SenderActor.SendText(peer, message, markDownText, mentions,
                autoDetect));
    }

    public void sendContact(Peer peer, String name, ArrayList<String> phones, ArrayList<String> emails, String base64photo) {
        sendMessageActor.send(new SenderActor.SendContact(peer, phones, emails, name, base64photo));
    }

    public void sendPhoto(@NotNull Peer peer, @NotNull String fileName, int w, int h, @Nullable FastThumb fastThumb,
                          @NotNull String descriptor) {
        FileSystemReference reference = Storage.fileFromDescriptor(descriptor);
        sendMessageActor.send(new SenderActor.SendPhoto(peer, fastThumb,
                descriptor,
                fileName, reference.getSize(), w, h));
    }

    public void sendVideo(Peer peer, String fileName, int w, int h, int duration,
                          FastThumb fastThumb, String descriptor) {
        FileSystemReference reference = Storage.fileFromDescriptor(descriptor);
        sendMessageActor.send(new SenderActor.SendVideo(peer, fileName, w, h, duration,
                fastThumb, descriptor, reference.getSize()));
    }

    public void sendAudio(@NotNull Peer peer, @NotNull String fileName, int duration,
                          @NotNull String descriptor) {
        FileSystemReference reference = Storage.fileFromDescriptor(descriptor);
        sendMessageActor.send(new SenderActor.SendAudio(peer,
                descriptor,
                fileName, reference.getSize(), duration));
    }

    public void sendLoacation(@NotNull Peer peer,
                              @NotNull Double longitude, @NotNull Double latitude,
                              @Nullable String street, @Nullable String place) {
        sendMessageActor.send(new SenderActor.SendLocation(peer, longitude, latitude, street, place));
    }

    public void sendJson(@NotNull Peer peer,
                         @NotNull JsonContent content) {
        sendMessageActor.send(new SenderActor.SendJson(peer, content));
    }

    public void updateJson(Peer peer, long rid, JsonContent json) {
        ApiPeer apiPeer = new ApiPeer(peer.getPeerType() == PeerType.PRIVATE ? ApiPeerType.PRIVATE : ApiPeerType.GROUP, peer.getPeerId());
        ApiJsonMessage jsonMessage = new ApiJsonMessage(json.getRawJson());
        updates().onUpdateReceived(new ChangeContent(new UpdateMessageContentChanged(apiPeer, rid, jsonMessage)));

    }

    public void sendDocument(Peer peer, String fileName, String mimeType, FastThumb fastThumb,
                             String descriptor) {
        FileSystemReference reference = Storage.fileFromDescriptor(descriptor);
        sendMessageActor.send(new SenderActor.SendDocument(peer, fileName, mimeType,
                reference.getSize(), reference.getDescriptor(), fastThumb));
    }

    public void sendSticker(@NotNull Peer peer,
                            @NotNull Sticker sticker) {
        sendMessageActor.send(new SenderActor.SendSticker(peer, sticker));
    }

    public void saveReadState(Peer peer, long lastReadDate) {
        preferences().putLong("read_state_" + peer.getUnuqueId(), lastReadDate);
    }

    public long loadReadState(Peer peer) {
        return preferences().getLong("read_state_" + peer.getUnuqueId(), 0);
    }

    public void saveDraft(Peer peer, String draft) {
        context().getSettingsModule().setStringValue("drafts_" + peer.getUnuqueId(), draft);
    }

    public String loadDraft(Peer peer) {
        String res = context().getSettingsModule().getStringValue("drafts_" + peer.getUnuqueId(), null);
        if (res == null) {
            return "";
        } else {
            return res;
        }
    }

    public Command<Boolean> deleteChat(final Peer peer) {
        return new Command<Boolean>() {
            @Override
            public void start(final CommandCallback<Boolean> callback) {
                ApiOutPeer outPeer;
                final ApiPeer apiPeer = buildApiPeer(peer);
                if (peer.getPeerType() == PeerType.PRIVATE) {
                    User user = users().getValue(peer.getPeerId());
                    if (user == null) {
                        runOnUiThread(new Runnable() {
                            @Override
                            public void run() {
                                callback.onError(new RpcInternalException());
                            }
                        });
                        return;
                    }
                    outPeer = new ApiOutPeer(ApiPeerType.PRIVATE, user.getUid(),
                            user.getAccessHash());
                } else if (peer.getPeerType() == PeerType.GROUP) {
                    Group group = groups().getValue(peer.getPeerId());
                    if (group == null) {
                        runOnUiThread(new Runnable() {
                            @Override
                            public void run() {
                                callback.onError(new RpcInternalException());
                            }
                        });
                        return;
                    }
                    outPeer = new ApiOutPeer(ApiPeerType.GROUP, group.getGroupId(),
                            group.getAccessHash());
                } else {
                    runOnUiThread(new Runnable() {
                        @Override
                        public void run() {
                            callback.onError(new RpcInternalException());
                        }
                    });
                    return;
                }
                request(new RequestDeleteChat(outPeer), new RpcCallback<ResponseSeq>() {
                    @Override
                    public void onResult(ResponseSeq response) {
                        updates().onUpdateReceived(new SeqUpdate(response.getSeq(),
                                response.getState(),
                                UpdateChatDelete.HEADER,
                                new UpdateChatDelete(apiPeer).toByteArray()));
                        runOnUiThread(new Runnable() {
                            @Override
                            public void run() {
                                callback.onResult(true);
                            }
                        });
                    }

                    @Override
                    public void onError(final RpcException e) {
                        runOnUiThread(new Runnable() {
                            @Override
                            public void run() {
                                callback.onError(e);
                            }
                        });
                    }
                });
            }
        };
    }

    public Command<Boolean> clearChat(final Peer peer) {
        return new Command<Boolean>() {
            @Override
            public void start(final CommandCallback<Boolean> callback) {
                ApiOutPeer outPeer;
                final ApiPeer apiPeer;
                if (peer.getPeerType() == PeerType.PRIVATE) {
                    User user = users().getValue(peer.getPeerId());
                    if (user == null) {
                        runOnUiThread(new Runnable() {
                            @Override
                            public void run() {
                                callback.onError(new RpcInternalException());
                            }
                        });
                        return;
                    }
                    outPeer = new ApiOutPeer(ApiPeerType.PRIVATE, user.getUid(),
                            user.getAccessHash());
                    apiPeer = new ApiPeer(ApiPeerType.PRIVATE,
                            user.getUid());
                } else if (peer.getPeerType() == PeerType.GROUP) {
                    Group group = groups().getValue(peer.getPeerId());
                    if (group == null) {
                        runOnUiThread(new Runnable() {
                            @Override
                            public void run() {
                                callback.onError(new RpcInternalException());
                            }
                        });
                        return;
                    }
                    outPeer = new ApiOutPeer(ApiPeerType.GROUP, group.getGroupId(),
                            group.getAccessHash());
                    apiPeer = new ApiPeer(ApiPeerType.GROUP,
                            group.getGroupId());
                } else {
                    runOnUiThread(new Runnable() {
                        @Override
                        public void run() {
                            callback.onError(new RpcInternalException());
                        }
                    });
                    return;
                }
                request(new RequestClearChat(outPeer), new RpcCallback<ResponseSeq>() {
                    @Override
                    public void onResult(ResponseSeq response) {
                        updates().onUpdateReceived(new SeqUpdate(response.getSeq(),
                                response.getState(),
                                UpdateChatClear.HEADER,
                                new UpdateChatClear(apiPeer).toByteArray()));
                        runOnUiThread(new Runnable() {
                            @Override
                            public void run() {
                                callback.onResult(true);
                            }
                        });
                    }

                    @Override
                    public void onError(final RpcException e) {
                        runOnUiThread(new Runnable() {
                            @Override
                            public void run() {
                                callback.onError(e);
                            }
                        });
                    }
                });
            }
        };
    }

    public Command<Boolean> addReaction(final Peer peer, final long rid, final String reaction) {
        return new Command<Boolean>() {
            @Override
            public void start(final CommandCallback<Boolean> callback) {
                ApiOutPeer outPeer;
                final ApiPeer apiPeer;
                if (peer.getPeerType() == PeerType.PRIVATE) {
                    User user = users().getValue(peer.getPeerId());
                    if (user == null) {
                        runOnUiThread(new Runnable() {
                            @Override
                            public void run() {
                                callback.onError(new RpcInternalException());
                            }
                        });
                        return;
                    }
                    outPeer = new ApiOutPeer(ApiPeerType.PRIVATE, user.getUid(),
                            user.getAccessHash());
                    apiPeer = new ApiPeer(ApiPeerType.PRIVATE,
                            user.getUid());
                } else if (peer.getPeerType() == PeerType.GROUP) {
                    Group group = groups().getValue(peer.getPeerId());
                    if (group == null) {
                        runOnUiThread(new Runnable() {
                            @Override
                            public void run() {
                                callback.onError(new RpcInternalException());
                            }
                        });
                        return;
                    }
                    outPeer = new ApiOutPeer(ApiPeerType.GROUP, group.getGroupId(),
                            group.getAccessHash());
                    apiPeer = new ApiPeer(ApiPeerType.GROUP,
                            group.getGroupId());
                } else {
                    runOnUiThread(new Runnable() {
                        @Override
                        public void run() {
                            callback.onError(new RpcInternalException());
                        }
                    });
                    return;
                }

                request(new RequestMessageSetReaction(outPeer, rid, reaction), new RpcCallback<ResponseReactionsResponse>() {
                    @Override
                    public void onResult(ResponseReactionsResponse response) {
                        updates().onSeqUpdateReceived(response.getSeq(),
                                response.getState(),
                                new UpdateReactionsUpdate(apiPeer, rid, response.getReactions()));

                        updates().executeAfter(response.getSeq(),
                                new Runnable() {
                                    @Override
                                    public void run() {
                                        runOnUiThread(new Runnable() {
                                            @Override
                                            public void run() {
                                                callback.onResult(true);
                                            }
                                        });
                                    }
                                });
                    }

                    @Override
                    public void onError(final RpcException e) {
                        runOnUiThread(new Runnable() {
                            @Override
                            public void run() {
                                callback.onError(e);
                            }
                        });
                    }
                });
            }
        };
    }

    public Command<Boolean> removeReaction(final Peer peer, final long rid, final String reaction) {
        return new Command<Boolean>() {
            @Override
            public void start(final CommandCallback<Boolean> callback) {
                ApiOutPeer outPeer;
                final ApiPeer apiPeer;
                if (peer.getPeerType() == PeerType.PRIVATE) {
                    User user = users().getValue(peer.getPeerId());
                    if (user == null) {
                        runOnUiThread(new Runnable() {
                            @Override
                            public void run() {
                                callback.onError(new RpcInternalException());
                            }
                        });
                        return;
                    }
                    outPeer = new ApiOutPeer(ApiPeerType.PRIVATE, user.getUid(),
                            user.getAccessHash());
                    apiPeer = new ApiPeer(ApiPeerType.PRIVATE,
                            user.getUid());
                } else if (peer.getPeerType() == PeerType.GROUP) {
                    Group group = groups().getValue(peer.getPeerId());
                    if (group == null) {
                        runOnUiThread(new Runnable() {
                            @Override
                            public void run() {
                                callback.onError(new RpcInternalException());
                            }
                        });
                        return;
                    }
                    outPeer = new ApiOutPeer(ApiPeerType.GROUP, group.getGroupId(),
                            group.getAccessHash());
                    apiPeer = new ApiPeer(ApiPeerType.GROUP,
                            group.getGroupId());
                } else {
                    runOnUiThread(new Runnable() {
                        @Override
                        public void run() {
                            callback.onError(new RpcInternalException());
                        }
                    });
                    return;
                }

                request(new RequestMessageRemoveReaction(outPeer, rid, reaction), new RpcCallback<ResponseReactionsResponse>() {
                    @Override
                    public void onResult(ResponseReactionsResponse response) {
                        updates().onSeqUpdateReceived(response.getSeq(),
                                response.getState(),
                                new UpdateReactionsUpdate(apiPeer, rid, response.getReactions()));

                        updates().executeAfter(response.getSeq(),
                                new Runnable() {
                                    @Override
                                    public void run() {
                                        runOnUiThread(new Runnable() {
                                            @Override
                                            public void run() {
                                                callback.onResult(true);
                                            }
                                        });
                                    }
                                });
                    }

                    @Override
                    public void onError(final RpcException e) {
                        runOnUiThread(new Runnable() {
                            @Override
                            public void run() {
                                callback.onError(e);
                            }
                        });
                    }
                });
            }
        };
    }

    public Command<Boolean> favoriteChat(final Peer peer) {
        return new Command<Boolean>() {
            @Override
            public void start(final CommandCallback<Boolean> callback) {
                ApiOutPeer outPeer;
                if (peer.getPeerType() == PeerType.PRIVATE) {
                    User user = users().getValue(peer.getPeerId());
                    if (user == null) {
                        runOnUiThread(new Runnable() {
                            @Override
                            public void run() {
                                callback.onError(new RpcInternalException());
                            }
                        });
                        return;
                    }
                    outPeer = new ApiOutPeer(ApiPeerType.PRIVATE, user.getUid(),
                            user.getAccessHash());
                } else if (peer.getPeerType() == PeerType.GROUP) {
                    Group group = groups().getValue(peer.getPeerId());
                    if (group == null) {
                        runOnUiThread(new Runnable() {
                            @Override
                            public void run() {
                                callback.onError(new RpcInternalException());
                            }
                        });
                        return;
                    }
                    outPeer = new ApiOutPeer(ApiPeerType.GROUP, group.getGroupId(),
                            group.getAccessHash());
                } else {
                    runOnUiThread(new Runnable() {
                        @Override
                        public void run() {
                            callback.onError(new RpcInternalException());
                        }
                    });
                    return;
                }
                request(new RequestFavouriteDialog(outPeer), new RpcCallback<ResponseDialogsOrder>() {
                    @Override
                    public void onResult(ResponseDialogsOrder response) {
                        updates().onSeqUpdateReceived(response.getSeq(),
                                response.getState(),
                                new UpdateChatGroupsChanged(response.getGroups()));

                        updates().executeAfter(response.getSeq(),
                                new Runnable() {
                                    @Override
                                    public void run() {
                                        runOnUiThread(new Runnable() {
                                            @Override
                                            public void run() {
                                                callback.onResult(true);
                                            }
                                        });
                                    }
                                });
                    }

                    @Override
                    public void onError(final RpcException e) {
                        runOnUiThread(new Runnable() {
                            @Override
                            public void run() {
                                callback.onError(e);
                            }
                        });
                    }
                });
            }
        };
    }

    public Command<Boolean> unfavoriteChat(final Peer peer) {
        return new Command<Boolean>() {
            @Override
            public void start(final CommandCallback<Boolean> callback) {
                ApiOutPeer outPeer;
                if (peer.getPeerType() == PeerType.PRIVATE) {
                    User user = users().getValue(peer.getPeerId());
                    if (user == null) {
                        runOnUiThread(new Runnable() {
                            @Override
                            public void run() {
                                callback.onError(new RpcInternalException());
                            }
                        });
                        return;
                    }
                    outPeer = new ApiOutPeer(ApiPeerType.PRIVATE, user.getUid(),
                            user.getAccessHash());
                } else if (peer.getPeerType() == PeerType.GROUP) {
                    Group group = groups().getValue(peer.getPeerId());
                    if (group == null) {
                        runOnUiThread(new Runnable() {
                            @Override
                            public void run() {
                                callback.onError(new RpcInternalException());
                            }
                        });
                        return;
                    }
                    outPeer = new ApiOutPeer(ApiPeerType.GROUP, group.getGroupId(),
                            group.getAccessHash());
                } else {
                    runOnUiThread(new Runnable() {
                        @Override
                        public void run() {
                            callback.onError(new RpcInternalException());
                        }
                    });
                    return;
                }
                request(new RequestUnfavouriteDialog(outPeer), new RpcCallback<ResponseDialogsOrder>() {
                    @Override
                    public void onResult(ResponseDialogsOrder response) {
                        updates().onSeqUpdateReceived(response.getSeq(),
                                response.getState(),
                                new UpdateChatGroupsChanged(response.getGroups()));

                        updates().executeAfter(response.getSeq(),
                                new Runnable() {
                                    @Override
                                    public void run() {
                                        runOnUiThread(new Runnable() {
                                            @Override
                                            public void run() {
                                                callback.onResult(true);
                                            }
                                        });
                                    }
                                });
                    }

                    @Override
                    public void onError(final RpcException e) {
                        runOnUiThread(new Runnable() {
                            @Override
                            public void run() {
                                callback.onError(e);
                            }
                        });
                    }
                });
            }
        };
    }


    public Command<Boolean> archiveChat(final Peer peer) {
        return new Command<Boolean>() {
            @Override
            public void start(final CommandCallback<Boolean> callback) {
                final ApiOutPeer outPeer;
                if (peer.getPeerType() == PeerType.PRIVATE) {
                    User user = users().getValue(peer.getPeerId());
                    if (user == null) {
                        runOnUiThread(new Runnable() {
                            @Override
                            public void run() {
                                callback.onError(new RpcInternalException());
                            }
                        });
                        return;
                    }
                    outPeer = new ApiOutPeer(ApiPeerType.PRIVATE, user.getUid(),
                            user.getAccessHash());
                } else if (peer.getPeerType() == PeerType.GROUP) {
                    Group group = groups().getValue(peer.getPeerId());
                    if (group == null) {
                        runOnUiThread(new Runnable() {
                            @Override
                            public void run() {
                                callback.onError(new RpcInternalException());
                            }
                        });
                        return;
                    }
                    outPeer = new ApiOutPeer(ApiPeerType.GROUP, group.getGroupId(),
                            group.getAccessHash());
                } else {
                    runOnUiThread(new Runnable() {
                        @Override
                        public void run() {
                            callback.onError(new RpcInternalException());
                        }
                    });
                    return;
                }
                request(new RequestArchiveChat(outPeer), new RpcCallback<ResponseSeq>() {
                    @Override
                    public void onResult(ResponseSeq response) {

                        updates().executeAfter(response.getSeq(),
                                new Runnable() {
                                    @Override
                                    public void run() {
                                        runOnUiThread(new Runnable() {
                                            @Override
                                            public void run() {
                                                callback.onResult(true);
                                            }
                                        });
                                    }
                                });
                    }

                    @Override
                    public void onError(final RpcException e) {
                        runOnUiThread(new Runnable() {
                            @Override
                            public void run() {
                                callback.onError(e);
                            }
                        });
                    }
                });
            }
        };
    }

    public ConversationVM getConversationVM(Peer peer) {
        return new ConversationVM(peer, context());
    }

    public void markAsLoaded(Peer peer) {
        if (isPersistentEnabled()) {
            preferences().putBool("chat.state_" + peer, true);
        }
        getConversationVM(peer).getIsLoaded().change(true);
    }

    public boolean isLoaded(Peer peer) {
        return preferences().getBool("chat.state_" + peer, false);
    }

    public void resetModule() {
        // TODO: Implement
    }

    @Override
    public void onBusEvent(Event event) {

        // Need to be here as events can be sent when no actor is created yet.
        if (event instanceof PeerChatOpened) {
            Peer peer = ((PeerChatOpened) event).getPeer();
            assumeConvActor(peer);
            getRouter().onChatOpen(peer);
        } else if (event instanceof PeerChatClosed) {
            Peer peer = ((PeerChatClosed) event).getPeer();
            assumeConvActor(peer);
            getRouter().onChatClosed(peer);
        }
    }
}