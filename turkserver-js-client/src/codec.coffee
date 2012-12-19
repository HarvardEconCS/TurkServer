
class Codec
  # TODO: Can we generate this automatically from the Java code?
  
  @hitView = "view.hit"  
  @hitAccept = "accept.hit"
  @hitSubmit = "submit.hit"
  
  @connectExpAck = "startexp"
  @roundStartMsg = "roundstart"
  @doneExpMsg = "finishexp"
  
  # channels
  @expChanPrefix = "/experiment/"
  @expSvcPrefix = "/service/experiment/"

module.exports = Codec