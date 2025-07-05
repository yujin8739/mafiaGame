package com.mafia.game.shop.model.vo;

import java.util.Date;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;


@NoArgsConstructor
@AllArgsConstructor
@Data
public class Shop {

    private int artId;
    private String title;
    private String description;
    private String imagePath;
    private String sellerName;
    private String buyerName;
    private int price;
    private String status; // 판매중, 판매완료 등
    private Date uploadDate;

    // 기본 생성자 + Getter/Setter + toString() 생략 가능 (롬복 사용하면 @Data 사용 가능)
}