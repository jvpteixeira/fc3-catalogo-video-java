package codeflix.catalog.admin.infrastructure.services.impl;

import codeflix.catalog.admin.domain.Fixture;
import codeflix.catalog.admin.domain.resource.Resource;
import codeflix.catalog.admin.domain.video.VideoMediaType;
import com.google.api.gax.paging.Page;
import com.google.cloud.storage.Blob;
import com.google.cloud.storage.BlobId;
import com.google.cloud.storage.BlobInfo;
import com.google.cloud.storage.Storage;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.mockito.Mockito;

import java.util.List;

import static org.mockito.Mockito.*;

class GoogleCloudStorageServiceTest {
    private GoogleCloudStorageService target;

    private Storage storage;

    private final String bucket = "test";

    @BeforeEach
    public void setUp() {
        this.storage = Mockito.mock(Storage.class);
        this.target = new GoogleCloudStorageService(this.bucket, this.storage);
    }

    @Test
    void givenValidResource_whenCallsStore_shouldStoreIt() {
        final var expectedResource = Fixture.Videos.resource(VideoMediaType.THUMBNAIL);
        final var expectedName = expectedResource.name();

        final Blob blob = this.mockBlob(expectedResource);
        doReturn(blob).when(this.storage).create(any(BlobInfo.class), any());

        this.target.store(expectedName, expectedResource);

        final var capturer = ArgumentCaptor.forClass(BlobInfo.class);

        verify(this.storage).create(capturer.capture(), eq(expectedResource.content()));

        final var actualBlob = capturer.getValue();
        Assertions.assertEquals(this.bucket, actualBlob.getBlobId().getBucket());
        Assertions.assertEquals(expectedName, actualBlob.getBlobId().getName());
        Assertions.assertEquals(expectedResource.contentType(), actualBlob.getContentType());
        Assertions.assertEquals(expectedResource.checksum(), actualBlob.getCrc32cToHexString());
    }

    @Test
    void givenResource_whenCallsGet_shouldRetrieveIt() {
        final var expectedResource = Fixture.Videos.resource(VideoMediaType.THUMBNAIL);
        final var expectedName = expectedResource.name();

        final Blob blob = this.mockBlob(expectedResource);
        doReturn(blob).when(this.storage).get(this.bucket, expectedName);

        final var actualContent = this.target.get(expectedName).get();

        Assertions.assertEquals(expectedResource.checksum(), actualContent.checksum());
        Assertions.assertEquals(expectedResource.name(), actualContent.name());
        Assertions.assertEquals(expectedResource.content(), actualContent.content());
        Assertions.assertEquals(expectedResource.contentType(), actualContent.contentType());
    }

    @Test
    void givenInvalidResource_whenCallsGet_shouldRetrieveEmpty() {
        final var expectedResource = Fixture.Videos.resource(VideoMediaType.THUMBNAIL);
        final var expectedName = expectedResource.name();

        doReturn(null).when(this.storage).get(this.bucket, expectedName);

        final var actualContent = this.target.get(expectedName);

        Assertions.assertTrue(actualContent.isEmpty());
    }

    @Test
    void givenPrefix_whenCallsList_shouldRetrieveAll() {
        final var video = Fixture.Videos.resource(VideoMediaType.VIDEO);
        final var banner = Fixture.Videos.resource(VideoMediaType.BANNER);
        final var expectedNames = List.of(video.name(), banner.name());

        final var page = Mockito.mock(Page.class);

        final Blob blob1 = this.mockBlob(video);
        final Blob blob2 = this.mockBlob(banner);

        doReturn(List.of(blob1, blob2)).when(page).iterateAll();
        doReturn(page).when(this.storage).list(this.bucket, Storage.BlobListOption.prefix("it"));

        final var actualContent = this.target.list("it");

        Assertions.assertTrue(
                expectedNames.size() == actualContent.size()
                        && expectedNames.containsAll(actualContent)
        );
    }

    @Test
    void givenResource_whenCallsDeleteAll_shouldEmptyStorage() {
        final var expectedIds = List.of("item1", "item2");

        this.target.deleteAll(expectedIds);

        final var captor = ArgumentCaptor.forClass(List.class);

        verify(this.storage).delete(captor.capture());

        final var actualIds = ((List<BlobId>) captor.getValue()).stream()
                .map(BlobId::getName)
                .toList();

        Assertions.assertTrue(expectedIds.size() == actualIds.size() && actualIds.containsAll(expectedIds));
    }

    private Blob mockBlob(final Resource resource) {
        final var blob = Mockito.mock(Blob.class);
        when(blob.getBlobId()).thenReturn(BlobId.of(this.bucket, resource.name()));
        when(blob.getCrc32cToHexString()).thenReturn(resource.checksum());
        when(blob.getContent()).thenReturn(resource.content());
        when(blob.getContentType()).thenReturn(resource.contentType());
        when(blob.getName()).thenReturn(resource.name());
        return blob;
    }
}