//
//  Generated by the J2ObjC translator.  DO NOT EDIT!
//  source: /Users/ex3ndr/Develop/actor-model/library/actor-cocoa-base/build/java/im/actor/model/api/rpc/ResponseGetContacts.java
//


#line 1 "/Users/ex3ndr/Develop/actor-model/library/actor-cocoa-base/build/java/im/actor/model/api/rpc/ResponseGetContacts.java"

#include "IOSClass.h"
#include "IOSPrimitiveArray.h"
#include "J2ObjC_source.h"
#include "im/actor/model/api/User.h"
#include "im/actor/model/api/rpc/ResponseGetContacts.h"
#include "im/actor/model/droidkit/bser/Bser.h"
#include "im/actor/model/droidkit/bser/BserObject.h"
#include "im/actor/model/droidkit/bser/BserValues.h"
#include "im/actor/model/droidkit/bser/BserWriter.h"
#include "im/actor/model/network/parser/Response.h"
#include "java/io/IOException.h"
#include "java/util/ArrayList.h"
#include "java/util/List.h"

@interface ImActorModelApiRpcResponseGetContacts () {
 @public
  id<JavaUtilList> users_;
  jboolean isNotChanged__;
}

@end

J2OBJC_FIELD_SETTER(ImActorModelApiRpcResponseGetContacts, users_, id<JavaUtilList>)


#line 24
@implementation ImActorModelApiRpcResponseGetContacts


#line 27
+ (ImActorModelApiRpcResponseGetContacts *)fromBytesWithByteArray:(IOSByteArray *)data {
  return ImActorModelApiRpcResponseGetContacts_fromBytesWithByteArray_(data);
}


#line 34
- (instancetype)initWithJavaUtilList:(id<JavaUtilList>)users
                         withBoolean:(jboolean)isNotChanged {
  ImActorModelApiRpcResponseGetContacts_initWithJavaUtilList_withBoolean_(self, users, isNotChanged);
  return self;
}


#line 39
- (instancetype)init {
  ImActorModelApiRpcResponseGetContacts_init(self);
  return self;
}


#line 43
- (id<JavaUtilList>)getUsers {
  return self->users_;
}

- (jboolean)isNotChanged {
  return self->isNotChanged__;
}


#line 52
- (void)parseWithBSBserValues:(BSBserValues *)values {
  id<JavaUtilList> _users = new_JavaUtilArrayList_init();
  for (jint i = 0; i < [((BSBserValues *) nil_chk(values)) getRepeatedCountWithInt:1]; i++) {
    [_users addWithId:new_ImActorModelApiUser_init()];
  }
  self->users_ = [values getRepeatedObjWithInt:1 withJavaUtilList:_users];
  self->isNotChanged__ = [values getBoolWithInt:2];
}


#line 62
- (void)serializeWithBSBserWriter:(BSBserWriter *)writer {
  [((BSBserWriter *) nil_chk(writer)) writeRepeatedObjWithInt:1 withJavaUtilList:self->users_];
  [writer writeBoolWithInt:2 withBoolean:self->isNotChanged__];
}


#line 68
- (NSString *)description {
  NSString *res = @"tuple GetContacts{";
  res = JreStrcat("$C", res, '}');
  return res;
}


#line 75
- (jint)getHeaderKey {
  return ImActorModelApiRpcResponseGetContacts_HEADER;
}

@end


#line 27
ImActorModelApiRpcResponseGetContacts *ImActorModelApiRpcResponseGetContacts_fromBytesWithByteArray_(IOSByteArray *data) {
  ImActorModelApiRpcResponseGetContacts_initialize();
  
#line 28
  return ((ImActorModelApiRpcResponseGetContacts *) BSBser_parseWithBSBserObject_withByteArray_(new_ImActorModelApiRpcResponseGetContacts_init(), data));
}

void ImActorModelApiRpcResponseGetContacts_initWithJavaUtilList_withBoolean_(ImActorModelApiRpcResponseGetContacts *self, id<JavaUtilList> users, jboolean isNotChanged) {
  (void) ImActorModelNetworkParserResponse_init(self);
  
#line 35
  self->users_ = users;
  self->isNotChanged__ = isNotChanged;
}


#line 34
ImActorModelApiRpcResponseGetContacts *new_ImActorModelApiRpcResponseGetContacts_initWithJavaUtilList_withBoolean_(id<JavaUtilList> users, jboolean isNotChanged) {
  ImActorModelApiRpcResponseGetContacts *self = [ImActorModelApiRpcResponseGetContacts alloc];
  ImActorModelApiRpcResponseGetContacts_initWithJavaUtilList_withBoolean_(self, users, isNotChanged);
  return self;
}


#line 39
void ImActorModelApiRpcResponseGetContacts_init(ImActorModelApiRpcResponseGetContacts *self) {
  (void) ImActorModelNetworkParserResponse_init(self);
}


#line 39
ImActorModelApiRpcResponseGetContacts *new_ImActorModelApiRpcResponseGetContacts_init() {
  ImActorModelApiRpcResponseGetContacts *self = [ImActorModelApiRpcResponseGetContacts alloc];
  ImActorModelApiRpcResponseGetContacts_init(self);
  return self;
}

J2OBJC_CLASS_TYPE_LITERAL_SOURCE(ImActorModelApiRpcResponseGetContacts)
