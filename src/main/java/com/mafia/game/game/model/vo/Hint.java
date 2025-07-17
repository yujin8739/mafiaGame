package com.mafia.game.game.model.vo;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@AllArgsConstructor
@NoArgsConstructor
@Data
public class Hint {
    private int hintNo;
    private int jobNo;
    private String hint;
    private int percent;
}
