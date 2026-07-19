package com.LHZ.TripMate.dto.tts;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@AllArgsConstructor
@NoArgsConstructor
public class PhonemeItemDTO {
    private String phone;
    private long start;
    private long end;


}