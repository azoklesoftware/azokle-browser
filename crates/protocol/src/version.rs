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
use std::fmt;

#[derive(Debug, Clone, Copy, PartialEq, Eq, PartialOrd, Ord, Hash, Serialize, Deserialize)]
pub struct ProtocolVersion {
    pub major: u16,
    pub minor: u16,
    pub patch: u16,
}

impl ProtocolVersion {
    pub const CURRENT: Self = Self {
        major: 1,
        minor: 0,
        patch: 0,
    };

    pub const fn new(major: u16, minor: u16, patch: u16) -> Self {
        Self {
            major,
            minor,
            patch,
        }
    }

    /// Determines if an incoming protocol version is compatible with this runtime.
    /// Following SemVer rules: Same major version, and incoming <= current.
    pub fn is_compatible_with(&self, other: &Self) -> bool {
        self.major == other.major && self.minor >= other.minor
    }
}

impl fmt::Display for ProtocolVersion {
    fn fmt(&self, f: &mut fmt::Formatter<'_>) -> fmt::Result {
        write!(f, "{}.{}.{}", self.major, self.minor, self.patch)
    }
}

#[cfg(test)]
mod tests {
    use super::*;

    #[test]
    fn test_version_compatibility() {
        let v1_0 = ProtocolVersion::new(1, 0, 0);
        let v1_1 = ProtocolVersion::new(1, 1, 0);
        let v2_0 = ProtocolVersion::new(2, 0, 0);

        assert!(v1_1.is_compatible_with(&v1_0));
        assert!(v1_0.is_compatible_with(&v1_0));
        assert!(!v1_0.is_compatible_with(&v1_1));
        assert!(!v1_0.is_compatible_with(&v2_0));
    }
}
