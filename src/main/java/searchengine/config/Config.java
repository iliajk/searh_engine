package searchengine.config;

import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class Config {
    @JsonProperty("indexing-settings")
    private IndexingSettings indexingSettings;
}
