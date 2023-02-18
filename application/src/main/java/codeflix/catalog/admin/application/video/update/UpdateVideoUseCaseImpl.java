package codeflix.catalog.admin.application.video.update;

import codeflix.catalog.admin.domain._share.exceptions.InternalErrorException;
import codeflix.catalog.admin.domain._share.exceptions.NotFoundException;
import codeflix.catalog.admin.domain._share.exceptions.NotificationException;
import codeflix.catalog.admin.domain._share.validation.Error;
import codeflix.catalog.admin.domain._share.validation.ValidationHandler;
import codeflix.catalog.admin.domain._share.validation.handler.Notification;
import codeflix.catalog.admin.domain._share.value.object.Identifier;
import codeflix.catalog.admin.domain.castmember.gateway.CastMemberGateway;
import codeflix.catalog.admin.domain.castmember.value.object.CastMemberID;
import codeflix.catalog.admin.domain.category.gateway.CategoryGateway;
import codeflix.catalog.admin.domain.category.value.object.CategoryID;
import codeflix.catalog.admin.domain.genre.gateway.GenreGateway;
import codeflix.catalog.admin.domain.genre.value.object.GenreID;
import codeflix.catalog.admin.domain.video.*;

import java.time.Year;
import java.util.*;
import java.util.function.Function;
import java.util.stream.Collectors;

import static codeflix.catalog.admin.domain.video.VideoMediaType.*;

public class UpdateVideoUseCaseImpl extends UpdateVideoUseCase {
    private final VideoGateway videoGateway;
    private final CategoryGateway categoryGateway;
    private final GenreGateway genreGateway;
    private final CastMemberGateway castMemberGateway;
    private final MediaResourceGateway mediaResourceGateway;

    public UpdateVideoUseCaseImpl(
            final VideoGateway videoGateway,
            final CategoryGateway categoryGateway,
            final GenreGateway genreGateway,
            final CastMemberGateway castMemberGateway,
            final MediaResourceGateway mediaResourceGateway) {
        this.videoGateway = Objects.requireNonNull(videoGateway);
        this.categoryGateway = Objects.requireNonNull(categoryGateway);
        this.genreGateway = Objects.requireNonNull(genreGateway);
        this.castMemberGateway = Objects.requireNonNull(castMemberGateway);
        this.mediaResourceGateway = Objects.requireNonNull(mediaResourceGateway);
    }

    @Override
    public UpdateVideoOutput execute(final UpdateVideoCommand aCommand) {
        final VideoID anId = VideoID.from(aCommand.id());
        final Rating aRating = Rating.of(aCommand.rating()).orElse(null);
        final Year aLaunchDate = Optional.ofNullable(aCommand.launchedAt()).map(Year::of).orElse(null);
        final Set<CategoryID> categories = this.toIdentifier(aCommand.categories(), CategoryID::from);
        final Set<GenreID> genres = this.toIdentifier(aCommand.genres(), GenreID::from);
        final Set<CastMemberID> members = this.toIdentifier(aCommand.members(), CastMemberID::from);

        final Video aVideo = this.videoGateway.findById(anId)
                .orElseThrow(() -> NotFoundException.with(Video.class, anId));

        final Notification notification = Notification.create();
        notification.append(this.validateCategories(categories));
        notification.append(this.validateGenres(genres));
        notification.append(this.validateCastMembers(members));

        aVideo.update(
                aCommand.title(),
                aCommand.description(),
                aLaunchDate,
                aCommand.duration(),
                aCommand.opened(),
                aCommand.published(),
                aRating,
                categories,
                genres,
                members
        );

        aVideo.validate(notification);

        if (notification.hasError()) throw new NotificationException("Could not update Aggregate Video", notification);

        return UpdateVideoOutput.from(this.update(aCommand, aVideo));
    }

    private Video update(final UpdateVideoCommand aCommand, final Video aVideo) {
        final VideoID anId = aVideo.getId();

        try {
            final AudioVideoMedia aVideoMedia = aCommand.getVideo()
                    .map(it -> this.mediaResourceGateway.storeAudioVideo(anId, VideoResource.with(it, VIDEO)))
                    .orElse(null);
            final AudioVideoMedia aTrailer = aCommand.getTrailer()
                    .map(it -> this.mediaResourceGateway.storeAudioVideo(anId, VideoResource.with(it, TRAILER)))
                    .orElse(null);
            final ImageMedia aBanner = aCommand.getBanner()
                    .map(it -> this.mediaResourceGateway.storeImage(anId, VideoResource.with(it, BANNER)))
                    .orElse(null);
            final ImageMedia aThumbnail = aCommand.getThumbnail()
                    .map(it -> this.mediaResourceGateway.storeImage(anId, VideoResource.with(it, THUMBNAIL)))
                    .orElse(null);
            final ImageMedia aThumbnailHalf = aCommand.getThumbnailHalf()
                    .map(it -> this.mediaResourceGateway.storeImage(anId, VideoResource.with(it, THUMBNAIL_HALF)))
                    .orElse(null);

            return this.videoGateway.update(
                    aVideo.updateVideoMedia(aVideoMedia)
                            .updateTrailerMedia(aTrailer)
                            .updateBannerMedia(aBanner)
                            .updateThumbnailMedia(aThumbnail)
                            .updateThumbnailHalfMedia(aThumbnailHalf)
            );
        } catch (final Exception e) {
            throw InternalErrorException.with("An error on update video was observed [videoId:%s]".formatted(anId.getValue()), e);
        }
    }

    private ValidationHandler validateCategories(final Set<CategoryID> ids) {
        return this.validateAggregate("categories", ids, this.categoryGateway::existsByIds);
    }

    private ValidationHandler validateGenres(final Set<GenreID> ids) {
        return this.validateAggregate("genres", ids, this.genreGateway::existsByIds);
    }

    private ValidationHandler validateCastMembers(final Set<CastMemberID> ids) {
        return this.validateAggregate("cast members", ids, this.castMemberGateway::existsByIds);
    }

    private <T extends Identifier> ValidationHandler validateAggregate(
            final String aggregate,
            final Set<T> ids,
            final Function<Iterable<T>, List<T>> existsByIds
    ) {
        final Notification notification = Notification.create();
        if (ids == null || ids.isEmpty()) return notification;

        final List<T> retrievedIds = existsByIds.apply(ids);

        if (ids.size() != retrievedIds.size()) {
            final List<T> missingIds = new ArrayList<>(ids);
            missingIds.removeAll(retrievedIds);

            final String missingIdsMessage = missingIds.stream()
                    .map(T::getValue)
                    .collect(Collectors.joining(", "));

            notification.append(new Error("Some %s could not be found: %s".formatted(aggregate, missingIdsMessage)));
        }

        return notification;
    }

    private <T> Set<T> toIdentifier(final Set<String> ids, final Function<String, T> mapper) {
        return ids.stream()
                .map(mapper)
                .collect(Collectors.toSet());
    }
}
