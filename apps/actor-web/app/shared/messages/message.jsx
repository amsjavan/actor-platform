var React = require('react');

var Message = React.createClass({
  propTypes: {
    message: React.PropTypes.object.isRequired
  },
  render: function() {
    var message = this.props.message;

    return (
      <div className="message__body" flex>
        <Message.State message={message}/>
        <Message.Content content={message.content}/>
      </div>
    );
  }
});

Message.Content = React.createClass({
  markedOptions: {
    sanitize: true,
    breaks: true,
    highlight: function (code) {
      return hljs.highlightAuto(code).value;
    }
  },
  propTypes: {
    content: React.PropTypes.object.isRequired
  },
  render: function() {
    var content = this.props.content;

    switch(content.content) {
      case 'text':
        var renderedText = marked(content.text, this.markedOptions);
        return (
          <p dangerouslySetInnerHTML={{__html: renderedText}}></p>
        );
      case 'document':
        return(
          <div className="message__document">
            <p>
              <img src="assets/img/icons/ic_attach_file_24px.svg"></img>
              <a href={content.fileUrl}>{content.fileName}</a>
            </p>
          </div>
        );
      case 'photo':
        var original = null;
        if (content.fileUrl) {
          original = <img className="message__photo__original"
                          width={ content.w }
                          height={ content.h }
                          src={content.fileUrl}/>
        }

        return(
          <div className="message__photo">
            {original}
            <img className="message__photo__preview"
                 width={content.w}
                 height={content.h}
                 src={content.preview}/>
          </div>
        );
      case 'service':
        return(
          <p className="service"
             flex>{content.text}</p>
        );
      case 'default':
        return(
          <p className="unsupported">
            Данный вид контента на данный момент не поддерживается.
          </p>
        );
    }
  }
});

Message.State = React.createClass({
  propTypes: {
    message: React.PropTypes.object.isRequired
  },
  render: function() {
    var message = this.props.message;

    if (message.content.content == 'service') {
      return null;
    } else {
      var icon = null;

      switch(message.state) {
        case 'pending':
          icon = <img src="assets/img/icons/ic_access_time_24px.svg" className="status status--penging"/>;
          break;
        case 'sent':
          icon = <img src="assets/img/icons/ic_done_24px.svg" className="status status--sent"/>;
          break;
        case 'received':
          icon = <img src="assets/img/icons/ic_done_all_24px.svg" className="status status--received"/>;
          break;
        case 'read':
          icon = <img src="assets/img/icons/ic_done_all_24px.svg" className="status status--read"/>;
          break;
        case 'error':
          icon = <img src="assets/img/icons/ic_report_problem_24px.svg" className="status status--error"/>;
          break;
      }

      return (
        <h3 layout="row" className="message__body__title">
          <span flex>{message.sender.title}</span>

          <div className="message__status">{icon}</div>

          <time className="message__body__timestamp">{message.date}</time>
        </h3>
      );
    }
  }
});

module.exports = Message;