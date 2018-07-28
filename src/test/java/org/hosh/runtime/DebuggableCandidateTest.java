package org.hosh.runtime;

import static org.assertj.core.api.Assertions.assertThat;

import org.junit.Test;

public class DebuggableCandidateTest {
	@Test
	public void stringValue() {
		assertThat(new DebuggableCandidate("aa")).hasToString("Candidate[value='aa']");
	}
}
