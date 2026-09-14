package ama.utilerias;

import java.util.List;
import lombok.Data;

@Data
public class TableResponse<T> {

    PageRender<T> page;
    private List<T> data;
}
