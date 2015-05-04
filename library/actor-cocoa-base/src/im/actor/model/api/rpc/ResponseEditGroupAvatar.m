//
//  Generated by the J2ObjC translator.  DO NOT EDIT!
//  source: /Users/ex3ndr/Develop/actor-model/library/actor-cocoa-base/build/java/im/actor/model/api/rpc/ResponseEditGroupAvatar.java
//


#line 1 "/Users/ex3ndr/Develop/actor-model/library/actor-cocoa-base/build/java/im/actor/model/api/rpc/ResponseEditGroupAvatar.java"

#include "IOSClass.h"
#include "IOSPrimitiveArray.h"
#include "J2ObjC_source.h"
#include "im/actor/model/api/Avatar.h"
#include "im/actor/model/api/rpc/ResponseEditGroupAvatar.h"
#include "im/actor/model/droidkit/bser/Bser.h"
#include "im/actor/model/droidkit/bser/BserObject.h"
#include "im/actor/model/droidkit/bser/BserValues.h"
#include "im/actor/model/droidkit/bser/BserWriter.h"
#include "im/actor/model/network/parser/Response.h"
#include "java/io/IOException.h"

@interface ImActorModelApiRpcResponseEditGroupAvatar () {
 @public
  ImActorModelApiAvatar *avatar_;
  jint seq_;
  IOSByteArray *state_;
  jlong date_;
}

@end

J2OBJC_FIELD_SETTER(ImActorModelApiRpcResponseEditGroupAvatar, avatar_, ImActorModelApiAvatar *)
J2OBJC_FIELD_SETTER(ImActorModelApiRpcResponseEditGroupAvatar, state_, IOSByteArray *)


#line 24
@implementation ImActorModelApiRpcResponseEditGroupAvatar


#line 27
+ (ImActorModelApiRpcResponseEditGroupAvatar *)fromBytesWithByteArray:(IOSByteArray *)data {
  return ImActorModelApiRpcResponseEditGroupAvatar_fromBytesWithByteArray_(data);
}


#line 36
- (instancetype)initWithImActorModelApiAvatar:(ImActorModelApiAvatar *)avatar
                                      withInt:(jint)seq
                                withByteArray:(IOSByteArray *)state
                                     withLong:(jlong)date {
  ImActorModelApiRpcResponseEditGroupAvatar_initWithImActorModelApiAvatar_withInt_withByteArray_withLong_(self, avatar, seq, state, date);
  return self;
}


#line 43
- (instancetype)init {
  ImActorModelApiRpcResponseEditGroupAvatar_init(self);
  return self;
}


#line 47
- (ImActorModelApiAvatar *)getAvatar {
  return self->avatar_;
}

- (jint)getSeq {
  return self->seq_;
}

- (IOSByteArray *)getState {
  return self->state_;
}

- (jlong)getDate {
  return self->date_;
}


#line 64
- (void)parseWithBSBserValues:(BSBserValues *)values {
  self->avatar_ = [((BSBserValues *) nil_chk(values)) getObjWithInt:1 withBSBserObject:new_ImActorModelApiAvatar_init()];
  self->seq_ = [values getIntWithInt:2];
  self->state_ = [values getBytesWithInt:3];
  self->date_ = [values getLongWithInt:4];
}


#line 72
- (void)serializeWithBSBserWriter:(BSBserWriter *)writer {
  if (self->avatar_ == nil) {
    @throw new_JavaIoIOException_init();
  }
  [((BSBserWriter *) nil_chk(writer)) writeObjectWithInt:1 withBSBserObject:self->avatar_];
  [writer writeIntWithInt:2 withInt:self->seq_];
  if (self->state_ == nil) {
    @throw new_JavaIoIOException_init();
  }
  [writer writeBytesWithInt:3 withByteArray:self->state_];
  [writer writeLongWithInt:4 withLong:self->date_];
}


#line 86
- (NSString *)description {
  NSString *res = @"tuple EditGroupAvatar{";
  res = JreStrcat("$C", res, '}');
  return res;
}


#line 93
- (jint)getHeaderKey {
  return ImActorModelApiRpcResponseEditGroupAvatar_HEADER;
}

@end


#line 27
ImActorModelApiRpcResponseEditGroupAvatar *ImActorModelApiRpcResponseEditGroupAvatar_fromBytesWithByteArray_(IOSByteArray *data) {
  ImActorModelApiRpcResponseEditGroupAvatar_initialize();
  
#line 28
  return ((ImActorModelApiRpcResponseEditGroupAvatar *) BSBser_parseWithBSBserObject_withByteArray_(new_ImActorModelApiRpcResponseEditGroupAvatar_init(), data));
}


#line 36
void ImActorModelApiRpcResponseEditGroupAvatar_initWithImActorModelApiAvatar_withInt_withByteArray_withLong_(ImActorModelApiRpcResponseEditGroupAvatar *self, ImActorModelApiAvatar *avatar, jint seq, IOSByteArray *state, jlong date) {
  (void) ImActorModelNetworkParserResponse_init(self);
  
#line 37
  self->avatar_ = avatar;
  self->seq_ = seq;
  self->state_ = state;
  self->date_ = date;
}


#line 36
ImActorModelApiRpcResponseEditGroupAvatar *new_ImActorModelApiRpcResponseEditGroupAvatar_initWithImActorModelApiAvatar_withInt_withByteArray_withLong_(ImActorModelApiAvatar *avatar, jint seq, IOSByteArray *state, jlong date) {
  ImActorModelApiRpcResponseEditGroupAvatar *self = [ImActorModelApiRpcResponseEditGroupAvatar alloc];
  ImActorModelApiRpcResponseEditGroupAvatar_initWithImActorModelApiAvatar_withInt_withByteArray_withLong_(self, avatar, seq, state, date);
  return self;
}


#line 43
void ImActorModelApiRpcResponseEditGroupAvatar_init(ImActorModelApiRpcResponseEditGroupAvatar *self) {
  (void) ImActorModelNetworkParserResponse_init(self);
}


#line 43
ImActorModelApiRpcResponseEditGroupAvatar *new_ImActorModelApiRpcResponseEditGroupAvatar_init() {
  ImActorModelApiRpcResponseEditGroupAvatar *self = [ImActorModelApiRpcResponseEditGroupAvatar alloc];
  ImActorModelApiRpcResponseEditGroupAvatar_init(self);
  return self;
}

J2OBJC_CLASS_TYPE_LITERAL_SOURCE(ImActorModelApiRpcResponseEditGroupAvatar)
