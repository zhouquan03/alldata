package datart.core.entity.ext;

import lombok.Data;

@Data
public class ViewBaseInfo {

    private String id;

    private String name;

    private String description;

    private String parentId;

    private Boolean isFolder;

    private Integer index;

    private SourceBaseInfo source;

}
