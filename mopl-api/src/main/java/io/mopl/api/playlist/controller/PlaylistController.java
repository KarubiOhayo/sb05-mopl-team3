package io.mopl.api.playlist.controller;

import io.mopl.api.playlist.dto.CursorResponsePlaylistDto;
import io.mopl.api.playlist.dto.PlaylistCreateRequest;
import io.mopl.api.playlist.dto.PlaylistDto;
import io.mopl.api.playlist.dto.PlaylistSearchRequest;
import io.mopl.api.playlist.service.PlaylistQueryService;
import io.mopl.api.playlist.service.PlaylistService;
import jakarta.validation.Valid;
import java.util.UUID;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.ModelAttribute;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequiredArgsConstructor
@RequestMapping("/api/playlists")
public class PlaylistController {

  private final PlaylistQueryService playlistQueryService;
  private final PlaylistService playlistService;

  @GetMapping
  public CursorResponsePlaylistDto findPlaylists(
      @Valid @ModelAttribute PlaylistSearchRequest request, @AuthenticationPrincipal UUID userId) {
    return playlistQueryService.findPlaylists(request, userId);
  }

  @PostMapping
  public ResponseEntity<PlaylistDto> createPlaylist(
      @Valid @RequestBody PlaylistCreateRequest request, @AuthenticationPrincipal UUID userId) {
    PlaylistDto playlistDto = playlistService.create(request, userId);
    return ResponseEntity.status(HttpStatus.CREATED).body(playlistDto);
  }
}
