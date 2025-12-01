package net.fosterlink.fosterlinkbackend.models.web.thread;

import lombok.AllArgsConstructor;
import lombok.Data;

@AllArgsConstructor
@Data
public class SearchThreadModel {

    private SearchBy searchBy;
    private String searchTerm;

}
