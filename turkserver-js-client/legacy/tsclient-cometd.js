function unescapeURL(s) {
    return decodeURIComponent(s.replace(/\+/g, "%20"));
}

function getURLParams() {
    var params = {}
    var m = window.location.href.match(/[\\?&]([^=]+)=([^&#]*)/g)
    if(m) {
        for(var i = 0; i < m.length; i++) {
            var a = m[i].match(/.([^=]+)=(.*)/)
            params[unescapeURL(a[1])] = unescapeURL(a[2])
        }
    }
    return params
}

// Configure POST parameters
var params = getURLParams();

// CometD callbacks
// First time connection
function _connectionInitialized() {
    console.log("handshake successful");
}


// (Re-)established connection, possibly first time
function _connectionEstablished() {
    $("#status").html("Connected");
    
    console.log("beginning connect")
    
    var hitId = params.hitId;
    var assignmentId = params.assignmentId;
    var workerId = params.workerId;

    if( !hitId ) {
        alert("The HIT data was not loaded properly. Please reload the HIT from your dashboard.");
    }
    else {
        $.cometd.batch(function() {
            _subscribe();

            if(assignmentId && assignmentId != "ASSIGNMENT_ID_NOT_AVAILABLE") {
                $.cometd.publish('/service/user', {
                    status : "accept",
                    hitId : hitId,
                    assignmentId : assignmentId,
                    workerId : workerId
                });
            } else {
                $.cometd.publish('/service/user', {
                    status : "view",
                    hitId : hitId
                });
            }

        });
    }
}

// Broken connection
function _connectionBroken() {
    $("#status").html("Connection Lost");
    // alert("It appears that we lost the connection to the server."
    // + "If this persists, please return the HIT.");
}

function userData(message) {
    var data = message.data;
    var status = data.status;

    console.log("Status: " + status);
    if( status == "startexp" ) {
	_subscribeExp(data.channel);
    }
}

function expData(message) {
    experimentData(message.data);
}

var _expSubscription;

function _subscribeExp(channel) {
    _expSubscription = $.cometd.subscribe(channel, expData);    
    console.log("Subscribed to exp channel " + channel);
}

var _userSubscription;

function _subscribe() {
    _userSubscription = $.cometd.subscribe('/service/user', userData);

    // Subscribe to any other necessary channels
    _subscribeData();
}

function _unsubscribe() {
    if(_userSubscription) {
        $.cometd.unsubscribe(_userSubscription);
    }    
    _userSubscription = null;

    if(_expSubscription) {
	$.cometd.unsubscribe(_expSubscrption);
    }
    _expSubscription = null;

    // Unsubscribe to any other channels
    _unsubscribeData();
}

function CometHelper(cookieName, contextPath) {

    var _connected = false;
    var _wasConnected = false;
    var _clientId;
    var _disconnecting;

    // Check for old state information
    var stateCookie = org.cometd.COOKIE ? org.cometd.COOKIE.get(cookieName) : null;
    var state = stateCookie ? org.cometd.JSON.fromJSON(stateCookie) : null;

    $.cometd.getExtension('reload').configure({
        cookieMaxAge : 10
    });

    // connect listener
    $.cometd.addListener('/meta/connect', function(message) {

        if($.cometd.isDisconnected())// Available since 1.1.2
        {
            return;
        }
        _wasConnected = _connected;
        _connected = message.successful;

        if(!_wasConnected && _connected) {
            _connectionEstablished();
        } else if(_wasConnected && !_connected) {
            _connectionBroken(); 
       }
    });
    
    // handshake listener to report client IDs
    $.cometd.addListener("/meta/handshake", function(message) {
        if(message.successful) {
            // TODO this stuff is not strictly necessary once things are working
            $('#previous').html(org.cometd.COOKIE.get('cometdID'));
            $('#current').html(message.clientId);

            org.cometd.COOKIE.set('cometdID', message.clientId, {
                'max-age' : 300,
                path : '/',
                expires : new Date(new Date().getTime() + 300 * 1000)         
            });
            
            _clientId = message.clientId;
            _connectionInitialized();
                   
        } else {
            $('#previous').html('Handshake Failed');
            $('#current').html('Handshake Failed');
        }

    });

    // disconnect listener
    $.cometd.addListener('/meta/disconnect', function(message) {
        if(message.successful) {
            _connected = false;
        }
    });

    /* Initialize CometD */
    var cometURL = location.protocol + "//" + location.host + contextPath + "/cometd";

    $.cometd.websocketEnabled = true;
    $.cometd.init({
        url : cometURL,
        logLevel : "info"
    });

    /* Setup reload extension */
    $(window).unload(function() {        
        
        if($.cometd.reload) {
            $.cometd.reload();
            // Save the application state only if the user was chatting
            
            if(_wasConnected && _clientId) {
                var expires = new Date();
                expires.setTime(expires.getTime() + 5 * 1000);
                org.cometd.COOKIE.set(cookieName, org.cometd.JSON.toJSON({
                    clientId : _clientId
                }), {
                    'max-age' : 5,
                    expires : expires
                });
            }
        } else {
            $.cometd.disconnect();
        }

    });
}
