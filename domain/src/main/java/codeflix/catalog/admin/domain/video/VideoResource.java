package codeflix.catalog.admin.domain.video;

import codeflix.catalog.admin.domain._share.value.object.ValueObject;
import codeflix.catalog.admin.domain.resource.Resource;

import java.util.Objects;

public class VideoResource extends ValueObject {
    private final Resource resource;
    private final VideoMediaType type;

    private VideoResource(final Resource resource, final VideoMediaType type) {
        this.resource = Objects.requireNonNull(resource);
        this.type = Objects.requireNonNull(type);
    }

    public static VideoResource with(final Resource aResource, final VideoMediaType aType) {
        return new VideoResource(aResource, aType);
    }

    public Resource resource() {
        return this.resource;
    }

    public VideoMediaType type() {
        return this.type;
    }

    @Override
    public boolean equals(final Object o) {
        if (this == o) return true;
        if (o == null || this.getClass() != o.getClass()) return false;
        final VideoResource that = (VideoResource) o;
        return this.type == that.type;
    }

    @Override
    public int hashCode() {
        return Objects.hash(this.type);
    }
}
