package org.hosh.runtime;

import org.junit.Test;

import static org.assertj.core.api.Assertions.*;

public class DebuggableCandidateTest {

	@Test
	public void stringValue() {
		assertThat(new DebuggableCandidate("aa")).hasToString("Candidate[value='aa']");
	}

}
