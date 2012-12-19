require './setup'

Codec = require './codec'
Util = require './util'

class TSClient
  
  @logLevel = "info"

  # Setup values
  @params = Util.getURLParams()
  @localMode = false
  
  @connected = false
  @wasConnected = false
  
  @clientId = undefined
  
  @expBroadcastSubscription = null
  @expServiceSubscription = null
  @userSubscription = null

  @connect_callback = undefined
  @disconnect_callback = undefined
  @error_callback = undefined  

  @startExperiment_cb = undefined    
  @startRound_cb = undefined
  @timeLimit_cb = undefined
  @finishExperiment_cb = undefined
  @clientError_cb = undefined
  @broadcastMessage_cb = undefined
  @serviceMessage_cb = undefined

  ###
  Callback registration
  ###

  @StartExperiment: (callback) ->
    @startExperiment_cb = callback

  @StartRound: (callback) ->
    @startRound_cb = callback

  @TimeLimit: (callback) ->
    @timeLimit_cb = callback

  @FinishExperiment: (callback) ->
    @finishExperiment_cb = callback
    
  @ClientError: (callback) ->
    @clientError_cb = callback
    
  @BroadcastMessage: (callback) ->
    @broadcastMessage_cb = callback        

  @ServiceMessage: (callback) ->
    @serviceMessage_cb = callback        
    
  ###
  Other functions
  ###

  @hitIsViewing: ->
    @params.assignmentId and @params.assignmentId is "ASSIGNMENT_ID_NOT_AVAILABLE"

  @initLocal: ->
    # Fake init for running demoes
    @localMode = true
    
    @params.hitId = "DEMO_HIT"
    @params.assignmentId = "DEMO_ASSIGNMENT"
    @params.workerId = "DEMO_WORKER"

  @init: (cookieName, contextPath) ->
    # Check for old state information
      
    stateCookie = (if org.cometd.COOKIE then org.cometd.COOKIE.get(cookieName) else null)
    state = (if stateCookie then org.cometd.JSON.fromJSON(stateCookie) else null)
    $.cometd.getExtension("reload").configure 
      cookieMaxAge: 10
    
    # connect listener
    $.cometd.addListener "/meta/connect", (message) =>
      # Available since 1.1.2
      return if $.cometd.isDisconnected()
      
      @wasConnected = @connected
      @connected = message.successful
      
      if not @wasConnected and @connected
        @connectionEstablished()
      else if @wasConnected and not @connected
        @connectionBroken()
    
    # handshake listener to report client IDs
    $.cometd.addListener "/meta/handshake", (message) =>
      if message.successful        
        @clientId = message.clientId
        @connectionInitialized()
      else
        @error_callback?()
    
    # disconnect listener
    $.cometd.addListener "/meta/disconnect", (message) ->
      @connected = false if message.successful
    
    # Initialize CometD 
    cometURL = location.protocol + "//" + location.host + ":9876" + contextPath + "/cometd"
    $.cometd.websocketEnabled = true
    $.cometd.init
      url: cometURL
      logLevel: @logLevel
    
    # Setup reload extension 
    $(window).unload =>
      if $.cometd.reload
        $.cometd.reload()
        
        # Save the application state only if the user was chatting
        if @wasConnected and @clientId
          expires = new Date()
          expires.setTime expires.getTime() + 5 * 1000
          org.cometd.COOKIE.set cookieName, org.cometd.JSON.toJSON(clientId: @clientId),
            "max-age": 5
            expires: expires

      else
        $.cometd.disconnect()

  # CometD callbacks
  # First time connection
  @connectionInitialized: ->
    console.log "handshake successful"

  # (Re-)established connection, possibly first time
  @connectionEstablished: ->
    @connect_callback?()
    
    console.log "beginning connect"
    hitId = @params.hitId
    assignmentId = @params.assignmentId
    workerId = @params.workerId
    
    unless hitId
      alert "The HIT data was not loaded properly. Please reload the HIT from your dashboard."
    else
      $.cometd.batch =>
        @subscribe()
        if assignmentId and assignmentId isnt "ASSIGNMENT_ID_NOT_AVAILABLE"
          $.cometd.publish "/service/user",
            status: Codec.hitAccept
            hitId: hitId
            assignmentId: assignmentId
            workerId: workerId
        else
          $.cometd.publish "/service/user",
            status: Codec.hitView
            hitId: hitId

  # Broken connection
  @connectionBroken: ->
    @disconnect_callback?()
    # alert("It appears that we lost the connection to the server."
    # + "If this persists, please return the HIT.");
    
  @userData: (message) =>
    data = message.data
    status = data.status
    console.log "Status: " + status
    switch status
      when Codec.connectExpAck
        @subscribeExp data.channel
        @startExperiment_cb?()
      when Codec.roundStartMsg
        @startRound_cb? data.round
      when Codec.doneExpMsg
        @finishExperiment_cb?()
    
  @subscribeExp: (channel) ->
    @expServiceSubscription = $.cometd.subscribe
      Codec.expSvcPrefix + channel, (message) => @serviceMessage_cb? message.data
    @expBroadcastSubscription = $.cometd.subscribe
      Codec.expChanPrefix + channel, (message) => @broadcastMessage_cb? message.data
    console.log "Subscribed to exp channels " + channel
    
  @subscribe: ->
    @userSubscription = $.cometd.subscribe "/service/user", @userData
    
  @unsubscribe: ->
    $.cometd.unsubscribe @userSubscription if @userSubscription
    @userSubscription = null
    $.cometd.unsubscribe @expBroadcastSubscrption if @expBroadcastSubscription
    @expBroadcastSubscription = null    
    $.cometd.unsubscribe @expServiceSubscription if @expServiceSubscription
    @expServiceSubscription = null
      
  @sendExperimentBroadcast (msg) =>
    @channelSend @expBroadcastSubscription[0], msg
    
  @sendExperimentService (msg) =>
    @channelSend @expServiceSubscription[0], msg
  
  @channelSend: (channel, msg) ->
    unless @localMode
      $.cometd.publish channel, msg
    else
      console.log "Would send on channel " + channel + " message " + msg

module.exports = TSClient
