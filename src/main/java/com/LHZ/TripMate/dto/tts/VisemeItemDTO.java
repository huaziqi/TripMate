package com.LHZ.TripMate.dto.tts;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@AllArgsConstructor
@NoArgsConstructor
@Data
public class VisemeItemDTO {
    private String viseme;
    private long start;
    private long end;


}