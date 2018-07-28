package org.hosh.runtime;

import org.hosh.doc.Bug;
import org.jline.reader.Candidate;

@Bug(description = "just a workaround for missing toString() in Candidate")
public class DebuggableCandidate extends Candidate {
	public DebuggableCandidate(String value) {
		super(value);
	}

	@Override
	public String toString() {
		return String.format("Candidate[value='%s']", value());
	}
}
