package com.example.daifugo.game.domain;

public enum GamePhase {
    WAITING,
    PLAYING,
    FINISHED;

	public boolean isPlaying() {
		// TODO 自動生成されたメソッド・スタブ
		return this == PLAYING;
	}
    
    
}

