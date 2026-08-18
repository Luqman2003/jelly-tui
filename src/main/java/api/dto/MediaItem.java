package api.dto;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;

@JsonIgnoreProperties(ignoreUnknown = true)
public record MediaItem(@JsonProperty("Name") String name, @JsonProperty("Id") String id, @JsonProperty("CollectionType") String collectionType, @JsonProperty("Type") String type, @JsonProperty("IndexNumber") Integer indexNumber, @JsonProperty("ProductionYear") Integer productionYear) {}
