
class Codec
  # TODO: Can we generate this automatically from the Java code?
  
  # TODO: update variable names that start with "status." to be the same as failsauce
  
  hitView: "view.hit"  
  hitAccept: "accept.hit"
  hitSubmit: "submit.hit"
  
  status_simultaneoussessions: "status.simultaneoussessions"
  status_sessionoverlap: "status.sessionoverlap";
  status_toomanysessions: "status.toomanysessions";
  errorMsg: "status.error"
  
  quizNeeded: "status.quizrequired"
  quizResults: "quiz.results"
  
  quizFail: "status.quizfailed"
  status_failsauce: "status.toomanyfails"
  
  usernameNeeded: "status.usernameneeded"
  usernameReply: "username.reply"
  
  connectLobbyAck: "status.connectlobby"
  connectExpAck: "status.connectexp"
  
  roundStartMsg: "roundstart"
  doneExpMsg: "finishexp"

  status_expfinished: "status.alreadyfinished"
  status_batchfinished: "status.batchfinished"    
  
  # channels
  expChanPrefix: "/experiment/"
  expSvcPrefix: "/service/experiment/"

module.exports = Codec
