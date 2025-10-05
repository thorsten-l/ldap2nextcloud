package l9g.app.ldap2nextcloud.client;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import java.util.Map;
import lombok.Getter;
import lombok.Setter;
import lombok.ToString;

@Getter
@Setter
@JsonIgnoreProperties(ignoreUnknown = true)
@ToString
public class OcsResult
{

  private Ocs ocs;

  @Getter
  @Setter
  @JsonIgnoreProperties(ignoreUnknown = true)
  @ToString
  public static class Ocs
  {

    private Meta meta;

    private Map<String, Object> data;

  }

  @Getter
  @Setter
  @JsonIgnoreProperties(ignoreUnknown = true)
  @ToString
  public static class Meta
  {

    private String status;

    private int statuscode;

    private String message;

    private String totalitems;

    private String itemsperpage;

  }

}
