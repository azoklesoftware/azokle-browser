//! Unsafe utility functions for working with flatbuffers and other low-level operations.

use crate::filters::flatbuffer_generated::fb;

// Minimum alignment for the beginning of the flatbuffer data.
const MIN_ALIGNMENT: usize = 8;

/// Converts a flatbuffers Vector to a slice.
/// # Safety
/// This function uses unsafe code to convert flatbuffer vector bytes to a slice.
/// It asserts the vector data is properly aligned, non-null, and sized.
#[inline(always)]
pub fn fb_vector_to_slice<T>(vector: flatbuffers::Vector<'_, T>) -> &[T] {
    let bytes = vector.bytes();
    if bytes.is_empty() {
        return &[];
    }

    const fn static_assert_alignment<T>() {
        // We can't use T with the size more than MIN_ALIGNMENT.
        // Since the beginning of flatbuffer data is aligned to that size,
        // the alignment of the data must be a divisor of MIN_ALIGNMENT.
        assert!(MIN_ALIGNMENT.is_multiple_of(std::mem::size_of::<T>()));
    }
    const { static_assert_alignment::<T>() };

    let size = std::mem::size_of::<T>();
    let align = std::mem::align_of::<T>();
    assert!(
        size > 0,
        "Zero-sized types are not supported in fb_vector_to_slice"
    );
    assert!(bytes.len().is_multiple_of(size));
    assert!((bytes.as_ptr() as usize).is_multiple_of(align));

    // SAFETY:
    // 1. `bytes` points to an initialized, contiguous FlatBuffers buffer whose lifetime is tied to `vector`.
    // 2. Pointer alignment is checked: `(bytes.as_ptr() as usize).is_multiple_of(align_of::<T>())`.
    // 3. Total byte length is an exact multiple of `size_of::<T>()`.
    // 4. `bytes` is confirmed non-empty above, so `bytes.as_ptr()` is non-null and valid for reads.
    unsafe { std::slice::from_raw_parts(bytes.as_ptr() as *const T, bytes.len() / size) }
}

// A safe wrapper around the flatbuffer data.
// It could be constructed from raw data (includes the flatbuffer verification)
// or from a builder that have just been used to construct the flatbuffer
// Invariants:
// 1. self.data() is properly verified flatbuffer contains the root object.
// 2. self.data() is aligned to MIN_ALIGNMENT bytes.
//    This is necessary for fb_vector_to_slice.
pub(crate) struct VerifiedFlatbufferMemory {
    // The buffer containing the flatbuffer data.
    raw_data: Vec<u8>,

    // The offset of the data in the buffer.
    // Must be aligned to MIN_ALIGNMENT bytes.
    start: usize,
}

impl VerifiedFlatbufferMemory {
    pub(crate) fn from_raw(data: &[u8]) -> Result<Self, flatbuffers::InvalidFlatbuffer> {
        let memory = Self::from_slice(data);

        // Verify that the data is a valid flatbuffer.
        let _ = fb::root_as_engine(memory.data())?;

        Ok(memory)
    }

    // Creates a new VerifiedFlatbufferMemory from a builder.
    // Skip the verification, the builder must contains a valid FilterList.
    pub(crate) fn from_builder(builder: flatbuffers::FlatBufferBuilder<'_>) -> Self {
        Self::from_slice(builder.finished_data())
    }

    // Properly align the buffer to MIN_ALIGNMENT bytes.
    pub(crate) fn from_slice(data: &[u8]) -> Self {
        let mut vec = Vec::with_capacity(data.len() + MIN_ALIGNMENT);
        let shift = vec.as_ptr() as usize % MIN_ALIGNMENT;

        let start = if shift == 0 {
            0
        } else {
            let shift = vec.as_ptr() as usize % MIN_ALIGNMENT;
            let padding = MIN_ALIGNMENT - shift;
            assert!(vec.capacity() >= padding);
            vec.splice(0..0, vec![0u8; padding]);
            padding
        };

        vec.extend_from_slice(data);
        assert!((vec.as_ptr() as usize + start).is_multiple_of(MIN_ALIGNMENT));

        let memory = Self {
            raw_data: vec,
            start,
        };
        assert!((memory.data().as_ptr() as usize).is_multiple_of(MIN_ALIGNMENT));
        memory
    }

    pub(crate) fn root(&self) -> fb::Engine<'_> {
        // In debug mode, re-verify root to catch any memory corruption early
        debug_assert!(
            fb::root_as_engine(self.data()).is_ok(),
            "Flatbuffer root verification failed"
        );

        // SAFETY:
        // `VerifiedFlatbufferMemory` enforces the invariant that `self.data()` was either validated
        // via `fb::root_as_engine(memory.data())?` in `from_raw()` or constructed by a valid
        // `FlatBufferBuilder` in `from_builder()`. The backing `raw_data` buffer is owned, aligned
        // to `MIN_ALIGNMENT`, and immutable during the lifetime of this reference.
        unsafe { fb::root_as_engine_unchecked(self.data()) }
    }

    pub fn data(&self) -> &[u8] {
        &self.raw_data[self.start..]
    }
}
