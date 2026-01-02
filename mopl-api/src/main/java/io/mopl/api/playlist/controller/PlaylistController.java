package io.mopl.api.playlist.controller;

import java.util.UUID;

import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.ModelAttribute;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import io.mopl.api.playlist.dto.CursorResponsePlaylistDto;
import io.mopl.api.playlist.dto.PlaylistSearchRequest;
import io.mopl.api.playlist.service.PlaylistQueryService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;

@RestController
@RequiredArgsConstructor
@RequestMapping("/api/playlists")
public class PlaylistController {

	private final PlaylistQueryService playlistQueryService;

	@GetMapping
	public CursorResponsePlaylistDto findPlaylists(
		@Valid @ModelAttribute PlaylistSearchRequest request,
		@AuthenticationPrincipal Jwt jwt
	) {
		UUID me = extractUserId(jwt);
		return playlistQueryService.findPlaylists(request, me);
	}

	// -- 헬퍼 메서드 --
	private UUID extractUserId(Jwt jwt) {
		if (jwt == null) {
			return null;
		}

		Object claimUserId = jwt.getClaims().get("userId");
		String raw = claimUserId != null ? String.valueOf(claimUserId) : jwt.getSubject();

		try {
			return raw != null ? UUID.fromString(raw) : null;
		} catch (IllegalArgumentException e) {
			return null;
		}
	}
}
