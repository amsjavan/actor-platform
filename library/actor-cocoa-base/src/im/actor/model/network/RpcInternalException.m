//
//  Generated by the J2ObjC translator.  DO NOT EDIT!
//  source: /Users/ex3ndr/Develop/actor-model/library/actor-cocoa-base/build/java/im/actor/model/network/RpcInternalException.java
//


#line 1 "/Users/ex3ndr/Develop/actor-model/library/actor-cocoa-base/build/java/im/actor/model/network/RpcInternalException.java"

#include "IOSClass.h"
#include "J2ObjC_source.h"
#include "im/actor/model/network/RpcException.h"
#include "im/actor/model/network/RpcInternalException.h"


#line 7
@implementation AMRpcInternalException


#line 8
- (instancetype)init {
  AMRpcInternalException_init(self);
  return self;
}

@end


#line 8
void AMRpcInternalException_init(AMRpcInternalException *self) {
  (void) AMRpcException_initWithNSString_withInt_withNSString_withBoolean_withByteArray_(self, @"INTERNAL_ERROR", 500, @"Internal server error", YES, nil);
}


#line 8
AMRpcInternalException *new_AMRpcInternalException_init() {
  AMRpcInternalException *self = [AMRpcInternalException alloc];
  AMRpcInternalException_init(self);
  return self;
}

J2OBJC_CLASS_TYPE_LITERAL_SOURCE(AMRpcInternalException)
