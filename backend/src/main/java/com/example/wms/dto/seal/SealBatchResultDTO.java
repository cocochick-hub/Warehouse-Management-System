package com.example.wms.dto.seal;

import lombok.AllArgsConstructor;
import lombok.Data;

import java.util.List;

@Data
@AllArgsConstructor
public class SealBatchResultDTO {

    private int successCount;
    private int failCount;
    private List<String> failReasons;
}
