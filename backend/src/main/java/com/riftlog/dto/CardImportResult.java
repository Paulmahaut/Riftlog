package com.riftlog.dto;

public record CardImportResult(
        int fetched,
        int created,
        int updated,
        long legends,
        long battlefields
) {
}
