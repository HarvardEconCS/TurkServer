
class Util

  @unescapeURL: (s) ->
    decodeURIComponent s.replace(/\+/g, "%20")
    
  @getURLParams: ->
    params = {}
    m = window.location.href.match(/[\\?&]([^=]+)=([^&#]*)/g)
    if m
      i = 0

      while i < m.length
        a = m[i].match(/.([^=]+)=(.*)/)
        params[@unescapeURL(a[1])] = @unescapeURL(a[2])
        i++
    params
    
module.exports = Util
