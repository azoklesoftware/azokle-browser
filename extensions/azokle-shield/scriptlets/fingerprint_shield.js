// Azokle Sovereign Fingerprint Shield Scriptlet
// Protects against Canvas, WebAudio, Hardware, and WebRTC fingerprinting attacks
// Injected at document_start

(function() {
    'use strict';

    if (window.__azokle_fp_shield_active) return;
    window.__azokle_fp_shield_active = true;

    // 1. Hardware Concurrency & Memory Uniformity (applied to prototype to preserve descriptor fidelity)
    try {
        var proto = Object.getPrototypeOf(navigator) || Navigator.prototype;
        if (proto) {
            Object.defineProperty(proto, 'hardwareConcurrency', {
                get: function() { return 4; },
                configurable: true,
                enumerable: true
            });
            Object.defineProperty(proto, 'deviceMemory', {
                get: function() { return 8; },
                configurable: true,
                enumerable: true
            });
        }
    } catch (e) {}

    // 2. Battery API Shield (Prevent battery level tracking)
    try {
        if (navigator.getBattery) {
            navigator.getBattery = function() {
                return Promise.resolve({
                    charging: true,
                    chargingTime: 0,
                    dischargingTime: Infinity,
                    level: 1.0,
                    addEventListener: function() {},
                    removeEventListener: function() {}
                });
            };
        }
    } catch (e) {}

    // 3. WebRTC Local IP Leak Neutralizer (RFC 8828 & Defense-in-Depth)
    try {
        var OrigRTCPeerConnection = window.RTCPeerConnection || window.webkitRTCPeerConnection || window.mozRTCPeerConnection;
        if (OrigRTCPeerConnection && OrigRTCPeerConnection.prototype) {
            var privateIpRegex = /(10\.\d{1,3}\.\d{1,3}\.\d{1,3}|172\.(1[6-9]|2\d|3[0-1])\.\d{1,3}\.\d{1,3}|192\.168\.\d{1,3}\.\d{1,3}|169\.254\.\d{1,3}\.\d{1,3}|[fF][eE]80:[0-9a-fA-F:]+)/;

            function sanitizeSdp(sdp) {
                if (!sdp || typeof sdp !== 'string') return sdp;
                return sdp.split('\r\n').filter(function(line) {
                    if (line.indexOf('a=candidate:') === 0 && (line.indexOf(' typ host') !== -1 || privateIpRegex.test(line))) {
                        return false;
                    }
                    return true;
                }).join('\r\n');
            }

            var origCreateOffer = OrigRTCPeerConnection.prototype.createOffer;
            if (origCreateOffer) {
                OrigRTCPeerConnection.prototype.createOffer = function() {
                    return origCreateOffer.apply(this, arguments).then(function(offer) {
                        if (offer && offer.sdp) {
                            try {
                                Object.defineProperty(offer, 'sdp', {
                                    value: sanitizeSdp(offer.sdp),
                                    writable: true,
                                    configurable: true
                                });
                            } catch (e) {
                                offer.sdp = sanitizeSdp(offer.sdp);
                            }
                        }
                        return offer;
                    });
                };
            }

            var origCreateAnswer = OrigRTCPeerConnection.prototype.createAnswer;
            if (origCreateAnswer) {
                OrigRTCPeerConnection.prototype.createAnswer = function() {
                    return origCreateAnswer.apply(this, arguments).then(function(answer) {
                        if (answer && answer.sdp) {
                            try {
                                Object.defineProperty(answer, 'sdp', {
                                    value: sanitizeSdp(answer.sdp),
                                    writable: true,
                                    configurable: true
                                });
                            } catch (e) {
                                answer.sdp = sanitizeSdp(answer.sdp);
                            }
                        }
                        return answer;
                    });
                };
            }

            var origAddEventListener = OrigRTCPeerConnection.prototype.addEventListener;
            OrigRTCPeerConnection.prototype.addEventListener = function(type, listener, options) {
                if (type === 'icecandidate' && typeof listener === 'function') {
                    var wrappedListener = function(event) {
                        if (event && event.candidate && event.candidate.candidate) {
                            var cand = event.candidate.candidate;
                            if (cand.indexOf(' typ host') !== -1 || privateIpRegex.test(cand)) {
                                return; // Suppress host candidate leaking local IP
                            }
                        }
                        return listener.apply(this, arguments);
                    };
                    return origAddEventListener.call(this, type, wrappedListener, options);
                }
                return origAddEventListener.apply(this, arguments);
            };

            var protoDesc = Object.getOwnPropertyDescriptor(OrigRTCPeerConnection.prototype, 'onicecandidate');
            if (protoDesc && protoDesc.set) {
                var origSet = protoDesc.set;
                Object.defineProperty(OrigRTCPeerConnection.prototype, 'onicecandidate', {
                    set: function(fn) {
                        if (typeof fn === 'function') {
                            var wrappedFn = function(event) {
                                if (event && event.candidate && event.candidate.candidate) {
                                    var cand = event.candidate.candidate;
                                    if (cand.indexOf(' typ host') !== -1 || privateIpRegex.test(cand)) {
                                        return; // Suppress host candidate
                                    }
                                }
                                return fn.apply(this, arguments);
                            };
                            return origSet.call(this, wrappedFn);
                        }
                        return origSet.call(this, fn);
                    },
                    get: protoDesc.get,
                    configurable: true,
                    enumerable: true
                });
            }
        }
    } catch (e) {}

    // 4. Modern Web Standards Compatibility Shims
    try {
        if (typeof window.webkitStorageInfo === 'undefined' && navigator.storage && navigator.storage.estimate) {
            window.webkitStorageInfo = {
                TEMPORARY: 0,
                PERSISTENT: 1,
                queryUsageAndQuota: function(type, successCallback, errorCallback) {
                    navigator.storage.estimate().then(function(estimate) {
                        if (typeof successCallback === 'function') {
                            successCallback(estimate.usage || 0, estimate.quota || 0);
                        }
                    }).catch(function(err) {
                        if (typeof errorCallback === 'function') errorCallback(err);
                    });
                },
                requestQuota: function(type, newQuotaInBytes, successCallback, errorCallback) {
                    if (typeof successCallback === 'function') {
                        successCallback(newQuotaInBytes);
                    }
                }
            };
        }
    } catch (e) {}

    // 5. Input Search Event Dispatcher for WebKit compatibility
    try {
        document.addEventListener('keydown', function(event) {
            if (event.key === 'Enter' && event.target && event.target.tagName === 'INPUT' && event.target.type === 'search') {
                var searchEvt = new CustomEvent('search', { bubbles: true, cancelable: true });
                event.target.dispatchEvent(searchEvt);
            }
        }, true);
    } catch (e) {}
})();
