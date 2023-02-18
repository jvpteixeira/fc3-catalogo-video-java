package codeflix.catalog.admin.infrastructure.video.models;

import com.fasterxml.jackson.annotation.JsonProperty;

public record AudioVideoMediaResponse(
        @JsonProperty("id") String id,
        @JsonProperty("checksum") String checksum,
        @JsonProperty("name") String name,
        @JsonProperty("location") String rawLocation,
        @JsonProperty("encoded_location") String encoded_location,
        @JsonProperty("status") String status
) {
}
