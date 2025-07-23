package com.mafia.game.security.config;


import java.util.Date;

import javax.crypto.SecretKey;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import io.jsonwebtoken.Claims;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.security.Keys;

@Component
public class JwtUtil {
		
	//토큰 비밀키  
	@Value("${jwt.secret:myScretKeyDefault123}")
	private String secret;
	 
	//토큰의 유효기간 - 보안을 위해 만료기간을 두고 사용 
	@Value("${jwt.expiration:86400000}")
	private long expiration;
	

	private SecretKey getSignKey() {
		return Keys.hmacShaKeyFor(secret.getBytes());
	}
	
	public String generateToken(String userName) {
		Date now = new Date();
		
		return Jwts.builder()
				   .setSubject(userName)
				   .setIssuedAt(now) // 발급시간 
				   .setExpiration(new Date(now.getTime()+expiration)) // 만료시간
				   .signWith(getSignKey()) //생성된 암호화키로 토큰에 디지털 서명 (토큰 위조 검증)
				   .compact(); //최종적으로 JWT 문자열 형태로 압축하여 반환
	}
	
	
	//토큰에서 userName 찾기
	public String getUserIdNameFromToken(String token) {
		
		Claims claims = Jwts.parserBuilder() 
							.setSigningKey(getSignKey()) //서명 검증 
							.build() //객체 생성 
							.parseClaimsJws(token) //서명,만료기한 등 유효 검증
							.getBody();//Claims 객체 추출
		
		return claims.getSubject();
	}
	
	
	//JWT 유효성확인
	public boolean vaildateToken(String token) {
		try {
			Jwts.parserBuilder()
				.setSigningKey(getSignKey())
				.build()
				.parseClaimsJws(token); 
			
			return true; //위에서 예외가 발생하지 않았다면 유효한 토큰 (true)
			
		}catch(Exception e) {
			return false;
		}
	} 
}