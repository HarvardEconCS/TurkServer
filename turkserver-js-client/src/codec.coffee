
class Codec
  # TODO: Can we generate this automatically from the Java code?
  
  @hitView = "view.hit"  
  @hitAccept = "accept.hit"
  @hitSubmit = "submit.hit"
  
  @quizNeeded = "quiz.required"
  @quizResults = "quiz.results"
  @quizFail = "quiz.failed"
  
  @usernameNeeded = "username"
  @usernameReply = "username.reply"
  
  @connectLobbyAck = "lobby"
  @connectExpAck = "startexp"
  
  @roundStartMsg = "roundstart"
  @doneExpMsg = "finishexp"
  
  @errorMsg = "error"
  
  # channels
  @expChanPrefix = "/experiment/"
  @expSvcPrefix = "/service/experiment/"

module.exports = Codec
