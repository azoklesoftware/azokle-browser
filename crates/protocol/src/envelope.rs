use crate::domains::MessagePayload;
use crate::endpoint::MessageEndpoint;
use crate::error::ProtocolError;
use crate::version::ProtocolVersion;
use serde::{Deserialize, Serialize};
use std::sync::atomic::{AtomicU64, Ordering};
use std::time::{SystemTime, UNIX_EPOCH};

static ENVELOPE_SEQ: AtomicU64 = AtomicU64::new(1);

#[derive(Debug, Clone, Serialize, Deserialize, PartialEq)]
pub struct ProtocolEnvelope {
    pub version: ProtocolVersion,
    pub message_id: String,
    pub correlation_id: Option<String>,
    pub timestamp_ms: u64,
    pub source: MessageEndpoint,
    pub target: MessageEndpoint,
    pub payload: MessagePayload,
}

impl ProtocolEnvelope {
    pub fn new(source: MessageEndpoint, target: MessageEndpoint, payload: MessagePayload) -> Self {
        let now = SystemTime::now()
            .duration_since(UNIX_EPOCH)
            .map(|d| d.as_millis() as u64)
            .unwrap_or(0);
        let seq = ENVELOPE_SEQ.fetch_add(1, Ordering::Relaxed);
        let message_id = format!("msg_{}_{}", now, seq);

        Self {
            version: ProtocolVersion::CURRENT,
            message_id,
            correlation_id: None,
            timestamp_ms: now,
            source,
            target,
            payload,
        }
    }

    pub fn with_correlation(mut self, correlation_id: &str) -> Self {
        self.correlation_id = Some(correlation_id.to_string());
        self
    }

    /// Serializes envelope to canonical JSON string.
    pub fn to_json(&self) -> Result<String, ProtocolError> {
        serde_json::to_string(self)
            .map_err(|e| ProtocolError::SerializationFailed(format!("JSON encode error: {}", e)))
    }

    /// Deserializes envelope from JSON string with version validation.
    pub fn from_json(json: &str) -> Result<Self, ProtocolError> {
        let envelope: Self = serde_json::from_str(json)
            .map_err(|e| ProtocolError::InvalidEnvelope(format!("JSON decode error: {}", e)))?;

        if !ProtocolVersion::CURRENT.is_compatible_with(&envelope.version) {
            return Err(ProtocolError::IncompatibleVersion(
                envelope.version.to_string(),
                ProtocolVersion::CURRENT.to_string(),
            ));
        }
        Ok(envelope)
    }

    /// Serializes envelope to compact binary CBOR bytes.
    pub fn to_cbor(&self) -> Result<Vec<u8>, ProtocolError> {
        let mut buf = Vec::new();
        ciborium::into_writer(self, &mut buf)
            .map_err(|e| ProtocolError::SerializationFailed(format!("CBOR encode error: {}", e)))?;
        Ok(buf)
    }

    /// Deserializes envelope from binary CBOR bytes with version validation.
    pub fn from_cbor(bytes: &[u8]) -> Result<Self, ProtocolError> {
        let envelope: Self = ciborium::from_reader(bytes)
            .map_err(|e| ProtocolError::InvalidEnvelope(format!("CBOR decode error: {}", e)))?;

        if !ProtocolVersion::CURRENT.is_compatible_with(&envelope.version) {
            return Err(ProtocolError::IncompatibleVersion(
                envelope.version.to_string(),
                ProtocolVersion::CURRENT.to_string(),
            ));
        }
        Ok(envelope)
    }
}

#[cfg(test)]
mod tests {
    use super::*;
    use crate::domains::{CommandPayload, RequestPayload, ResponsePayload};

    #[test]
    fn test_json_roundtrip() {
        let payload = MessagePayload::Command(CommandPayload::TabOpen {
            url: "https://search.azokle.com".to_string(),
            title: "New Tab".to_string(),
            is_private: false,
            is_tor: false,
            group_id: None,
            group_title: None,
        });

        let envelope = ProtocolEnvelope::new(MessageEndpoint::AndroidUI, MessageEndpoint::RustCore, payload)
            .with_correlation("req_100");

        let json = envelope.to_json().unwrap();
        let decoded = ProtocolEnvelope::from_json(&json).unwrap();

        assert_eq!(envelope, decoded);
        assert_eq!(decoded.correlation_id.as_deref(), Some("req_100"));
    }

    #[test]
    fn test_cbor_binary_roundtrip() {
        let payload = MessagePayload::Request(RequestPayload::AutosuggestQuery {
            query: "privacy browser".to_string(),
        });

        let envelope = ProtocolEnvelope::new(MessageEndpoint::IosUI, MessageEndpoint::RustCore, payload);
        let cbor = envelope.to_cbor().unwrap();
        let decoded = ProtocolEnvelope::from_cbor(&cbor).unwrap();

        assert_eq!(envelope, decoded);
    }
}
