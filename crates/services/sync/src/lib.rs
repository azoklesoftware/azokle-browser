// Copyright (c) 2026 The Azokle Authors.
//
// Licensed under the Apache License, Version 2.0 (the "License");
// you may not use this file except in compliance with the License.
// You may obtain a copy of the License at
//
//     http://www.apache.org/licenses/LICENSE-2.0
//
// Unless required by applicable law or agreed to in writing, software
// distributed under the License is distributed on an "AS IS" BASIS,
// WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
// See the License for the specific language governing permissions and
// limitations under the License.

use serde::{Deserialize, Serialize};

#[derive(Debug, Clone, Serialize, Deserialize)]
pub struct SyncDevice {
    pub device_id: String,
    pub device_name: String,
    pub public_key_b64: String,
    pub last_synced: u64,
}

#[derive(Debug, Clone, Serialize, Deserialize)]
pub struct EncryptedSyncPayload {
    pub version: u32,
    pub sender_device_id: String,
    pub encrypted_payload_b64: String,
    pub timestamp: u64,
}

pub struct SyncProtocol;

impl SyncProtocol {
    /// Performs create sync payload operation.
    pub fn create_sync_payload(device_id: &str, ciphertext_b64: &str) -> EncryptedSyncPayload {
        let now = std::time::SystemTime::now()
            .duration_since(std::time::UNIX_EPOCH)
            .map(|d| d.as_secs())
            .unwrap_or(0);

        EncryptedSyncPayload {
            version: 1,
            sender_device_id: device_id.to_string(),
            encrypted_payload_b64: ciphertext_b64.to_string(),
            timestamp: now,
        }
    }
}

#[cfg(test)]
mod tests {
    use super::*;

    #[test]
    fn test_sync_payload() {
        let payload = SyncProtocol::create_sync_payload("device_alpha", "encrypted_data_string");
        assert_eq!(payload.version, 1);
        assert_eq!(payload.sender_device_id, "device_alpha");
        assert_eq!(payload.encrypted_payload_b64, "encrypted_data_string");
    }
}
