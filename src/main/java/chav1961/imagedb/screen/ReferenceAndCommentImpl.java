package chav1961.imagedb.screen;

import java.net.URI;
import java.util.Objects;

import chav1961.purelib.ui.interfaces.ReferenceAndComment;

class ReferenceAndCommentImpl implements ReferenceAndComment {
	private URI		reference;
	private String	comment;

	ReferenceAndCommentImpl(final URI reference, final String comment) {
		this.reference = reference;
		this.comment = comment;
	}
	
	@Override
	public URI getReference() {
		return reference;
	}

	@Override
	public void setReference(final URI reference) {
		this.reference = reference;
	}

	@Override
	public String getComment() {
		return comment;
	}

	@Override
	public void setComment(String comment) {
		this.comment = comment;
	}

	@Override
	public int hashCode() {
		return Objects.hash(comment, reference);
	}

	@Override
	public boolean equals(Object obj) {
		if (this == obj)
			return true;
		if (obj == null)
			return false;
		if (getClass() != obj.getClass())
			return false;
		ReferenceAndCommentImpl other = (ReferenceAndCommentImpl) obj;
		return Objects.equals(comment, other.comment) && Objects.equals(reference, other.reference);
	}

	@Override
	public String toString() {
		return "ReferenceAndCommentImpl [reference=" + reference + ", comment=" + comment + "]";
	}
}
