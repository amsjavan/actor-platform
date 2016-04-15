/*
 * Copyright (C) 2015-2016 Actor LLC. <https://actor.im>
 */

import { isFunction, last } from 'lodash';
import React, { Component, PropTypes } from 'react';

import { getMessageState } from '../../utils/MessageUtils';

import DefaultMessageItem from './messages/MessageItem.react';

class MessageGroup extends Component {
  static contextTypes = {
    delegate: PropTypes.object.isRequired
  };

  static propTypes = {
    uid: PropTypes.number.isRequired,
    peer: PropTypes.object.isRequired,
    messages: PropTypes.array.isRequired,
    selected: PropTypes.object.isRequired,
    receiveDate: PropTypes.number.isRequired,
    readDate: PropTypes.number.isRequired,
    onSelect: PropTypes.func.isRequired
  };

  constructor(props, context) {
    super(props);

    const { dialog } = context.delegate.components;
    if (dialog && dialog.messages && isFunction(dialog.messages.message)) {
      this.components = {
        MessageItem: dialog.messages.message
      };
    } else {
      this.components = {
        MessageItem: DefaultMessageItem
      };
    }
  }

  shouldComponentUpdate(nextProps) {
    return this.props.messages[0] !== nextProps.messages[0] ||
           last(this.props.messages) !== last(nextProps.messages);
  }

  render() {
    const { MessageItem } = this.components;
    const { uid, peer, messages, selected, receiveDate, readDate } = this.props;

    const result = [];
    for (let i = 0; i < messages.length; i++) {
      const message = messages[i];
      const state = getMessageState(message, uid, receiveDate, readDate);

      result.push(
        <MessageItem
          key={message.sortKey}
          peer={peer}
          message={message}
          state={state}
          isShort={i !== 0}
          isSelected={selected.has(message.rid)}
          onSelect={this.props.onSelect}
        />
      )
    }

    return (
      <div>
        {result}
      </div>
    )
  }
}

export default MessageGroup;
