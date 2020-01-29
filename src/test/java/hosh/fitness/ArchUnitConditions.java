package hosh.fitness;

import com.tngtech.archunit.core.domain.JavaField;
import com.tngtech.archunit.lang.ArchCondition;
import com.tngtech.archunit.lang.ConditionEvents;
import com.tngtech.archunit.lang.SimpleConditionEvent;

public class ArchUnitConditions {

	private ArchUnitConditions() {
	}

	public static HaveAccesses haveAccesses() {
		return new HaveAccesses();
	}

	private static class HaveAccesses extends ArchCondition<JavaField> {

		public HaveAccesses() {
			super("be used used or removed");
		}

		@Override
		public void check(JavaField item, ConditionEvents events) {
			if (item.getAccessesToSelf().isEmpty()) {
				events.add(new SimpleConditionEvent(item, false, "unused @Mock " + item.getFullName()));
			}
		}
	}
}
