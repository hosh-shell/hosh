/*
 * MIT License
 *
 * Copyright (c) 2018-2026 Davide Angelocola
 *
 * Permission is hereby granted, free of charge, to any person obtaining a copy
 * of this software and associated documentation files (the "Software"), to deal
 * in the Software without restriction, including without limitation the rights
 * to use, copy, modify, merge, publish, distribute, sublicense, and/or sell
 * copies of the Software, and to permit persons to whom the Software is
 * furnished to do so, subject to the following conditions:
 *
 * The above copyright notice and this permission notice shall be included in all
 * copies or substantial portions of the Software.
 *
 * THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND, EXPRESS OR
 * IMPLIED, INCLUDING BUT NOT LIMITED TO THE WARRANTIES OF MERCHANTABILITY,
 * FITNESS FOR A PARTICULAR PURPOSE AND NONINFRINGEMENT. IN NO EVENT SHALL THE
 * AUTHORS OR COPYRIGHT HOLDERS BE LIABLE FOR ANY CLAIM, DAMAGES OR OTHER
 * LIABILITY, WHETHER IN AN ACTION OF CONTRACT, TORT OR OTHERWISE, ARISING FROM,
 * OUT OF OR IN CONNECTION WITH THE SOFTWARE OR THE USE OR OTHER DEALINGS IN THE
 * SOFTWARE.
 */
package hosh.runtime;

import hosh.doc.Bug;
import hosh.runtime.Compiler.CompileError;
import hosh.runtime.Compiler.Program;
import hosh.runtime.Compiler.Statement;
import hosh.spi.*;
import org.assertj.core.api.InstanceOfAssertFactories;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Disabled;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.List;
import java.util.Map;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.doReturn;

@ExtendWith(MockitoExtension.class)
class CompilerTest {

	@Mock(stubOnly = true)
	State state;

	@Mock(stubOnly = true)
	Command command;

	@Mock(stubOnly = true)
	Command anotherCommand;

	@Mock(stubOnly = true)
	CommandWrapper commandWrapper;

	@Mock(stubOnly = true)
	CommandResolver commandResolver;

	Compiler sut;

	@BeforeEach
	void createSut() {
		sut = new Compiler(commandResolver);
	}

	@Bug(issue = "https://github.com/dfa1/hosh/issues/26", description = "rejected by the compiler")
	@Test
	void incompletePipeline() {
		// Given
		doReturn(Optional.of(command)).when(commandResolver).tryResolve("ls");

		// When / Then
		assertThatThrownBy(() -> sut.compile("ls | take 2 | "))
				.isInstanceOf(CompileError.class)
				.hasMessage("line 1:12: incomplete pipeline near '|'");
	}

	@Bug(issue = "https://github.com/dfa1/hosh/issues/42", description = "rejected by the compiler")
	@Test
	void extraBraces() {
		// Given
		// (no setup)

		// When / Then
		assertThatThrownBy(() -> sut.compile("withTime { ls } } "))
				.isInstanceOf(CompileError.class)
				.hasMessage("line 1: unnecessary closing '}'");
	}

	@Test
	void pipelineOfCommandsWithoutArguments() {
		// Given
		doReturn(Optional.of(command)).when(commandResolver).tryResolve("ls");
		doReturn(Optional.of(anotherCommand)).when(commandResolver).tryResolve("count");

		// When
		Program result = sut.compile("ls | count");

		// Then
		assertThat(result.getStatements())
				.hasSize(1)
				.first().satisfies(statement -> assertThat(statement.getCommand()).isInstanceOf(PipelineCommand.class));
	}

	@Test
	void pipelineOfCommandsWithArguments() {
		// Given
		doReturn(Optional.of(command)).when(commandResolver).tryResolve("ls");
		doReturn(Optional.of(anotherCommand)).when(commandResolver).tryResolve("grep");

		// When
		Program result = sut.compile("ls /home | grep /regex/");

		// Then
		assertThat(result.getStatements()).hasSize(1);
		List<Statement> statements = result.getStatements();
		assertThat(statements)
				.hasSize(1)
				.first().satisfies(statement -> {
					assertThat(statement.getCommand()).isInstanceOf(PipelineCommand.class);
					assertThat(statement.getArguments()).isEmpty();
				});
	}

	@Test
	void commandWithConstant() {
		// Given
		doReturn(Optional.of(command)).when(commandResolver).tryResolve("cd");

		// When
		Program result = sut.compile("cd /tmp");

		// Then
		assertThat(result.getStatements())
				.hasSize(1)
				.first().satisfies(statement -> {
					assertThat(statement.getCommand()).isSameAs(command);
					assertThat(statement.getArguments())
							.hasSize(1)
							.first().satisfies(arg -> assertThat(arg).isInstanceOf(Compiler.Constant.class));
				});
	}

	@Test
	void commandWithVariable() {
		// Given
		doReturn(Optional.of(command)).when(commandResolver).tryResolve("cd");

		// When
		Program result = sut.compile("cd ${DIR}");

		// Then
		assertThat(result.getStatements())
				.hasSize(1)
				.first().satisfies(statement -> {
					assertThat(statement.getCommand()).isSameAs(command);
					assertThat(statement.getArguments())
							.hasSize(1)
							.first().satisfies(arg -> assertThat(arg).isInstanceOf(Compiler.Variable.class));
				});
	}

	@Test
	void commandWithVariableOrFallback() {
		// Given
		doReturn(Optional.of(command)).when(commandResolver).tryResolve("cd");

		// When
		Program result = sut.compile("cd ${DIR!/tmp}");

		// Then
		assertThat(result.getStatements())
				.hasSize(1)
				.first().satisfies(statement -> {
					assertThat(statement.getCommand()).isSameAs(command);
					assertThat(statement.getArguments())
							.hasSize(1)
							.first().satisfies(arg -> assertThat(arg).isInstanceOf(Compiler.VariableOrFallback.class));

				});
	}

	@Test
	void commandWithoutArguments() {
		// Given
		doReturn(Optional.of(command)).when(commandResolver).tryResolve("env");

		// When
		Program result = sut.compile("env");

		// Then
		assertThat(result.getStatements())
				.hasSize(1)
				.first().satisfies(statement -> {
					assertThat(statement.getCommand()).isSameAs(command);
					assertThat(statement.getArguments()).isEmpty();
				});
	}

	@Test
	void commandWithArgument() {
		// Given
		doReturn(Optional.of(command)).when(commandResolver).tryResolve("env");

		// When
		Program result = sut.compile("env --system");

		// Then
		assertThat(result.getStatements())
				.hasSize(1)
				.first().satisfies(statement -> {
					assertThat(statement.getCommand()).isSameAs(command);
					assertThat(statement.getArguments()).hasSize(1);
				});
	}

	@Test
	void commandWithArguments() {
		// Given
		doReturn(Optional.of(command)).when(commandResolver).tryResolve("git");

		// When
		Program result = sut.compile("git commit --amend");

		// Then
		assertThat(result.getStatements())
				.hasSize(1)
				.first().satisfies(statement -> {
					assertThat(statement.getCommand()).isSameAs(command);
					assertThat(statement.getArguments()).hasSize(2);
				});
	}

	@Test
	void commandNotRegisteredInAPipeline() {
		// Given
		doReturn(Optional.of(command)).when(commandResolver).tryResolve("env");

		// When / Then
		assertThatThrownBy(() -> sut.compile("env | env2"))
				.isInstanceOf(CompileError.class)
				.hasMessage("line 1: 'env2' unknown command");
	}

	@Test
	void wrappedCommand() {
		// Given
		doReturn(Optional.of(commandWrapper)).when(commandResolver).tryResolve("withTime");
		doReturn(Optional.of(command)).when(commandResolver).tryResolve("git");

		// When
		Program result = sut.compile("withTime -t -a { git push }");

		// Then
		assertThat(result.getStatements())
				.hasSize(1)
				.first().satisfies(statement -> {
					assertThat(statement.getLocation()).isEqualTo("withTime");
					assertThat(statement.getArguments()).hasSize(2);
				});
	}

	@Test
	void nestedWrappedCommands() {
		// Given
		doReturn(Optional.of(commandWrapper)).when(commandResolver).tryResolve("withTime");
		doReturn(Optional.of(command)).when(commandResolver).tryResolve("git");

		// When
		Program result = sut.compile("withTime { withTime { git push } }");

		// Then
		assertThat(result.getStatements())
				.hasSize(1)
				.first().satisfies(statement -> assertThat(statement.getCommand()).isInstanceOf(DefaultCommandDecorator.class));
	}

	@Test
	void commandWrapperUsedAsCommand() {
		// Given
		doReturn(Optional.of(commandWrapper)).when(commandResolver).tryResolve("withTime");

		// When / Then
		assertThatThrownBy(() -> sut.compile("withTime"))
				.isInstanceOf(CompileError.class)
				.hasMessage("line 1: 'withTime' is a command wrapper");
	}

	@Test
	void emptyCommandWrapper() {
		// Given
		doReturn(Optional.of(commandWrapper)).when(commandResolver).tryResolve("withTime");

		// When / Then
		assertThatThrownBy(() -> sut.compile("withTime { }"))
				.isInstanceOf(CompileError.class)
				.hasMessage("line 1: 'withTime' with empty wrapping statement");
	}

	@Test
	void commandUsedAsCommandWrapper() {
		// Given
		doReturn(Optional.of(command)).when(commandResolver).tryResolve("ls");
		doReturn(Optional.of(command)).when(commandResolver).tryResolve("grep");

		// When / Then
		assertThatThrownBy(() -> sut.compile("ls { grep pattern } "))
				.isInstanceOf(CompileError.class)
				.hasMessage("line 1: 'ls' is not a command wrapper");
	}

	@Test
	void unknownCommand() {
		// Given
		doReturn(Optional.empty()).when(commandResolver).tryResolve("ls");

		// When / Then
		assertThatThrownBy(() -> sut.compile("ls { grep pattern }"))
				.isInstanceOf(CompileError.class)
				.hasMessage("line 1: 'ls' unknown command wrapper");
	}

	@Test
	void commandWrapperAsProducer() {
		// Given
		doReturn(Optional.of(commandWrapper)).when(commandResolver).tryResolve("benchmark");
		doReturn(Optional.of(command)).when(commandResolver).tryResolve("ls");
		doReturn(Optional.of(anotherCommand)).when(commandResolver).tryResolve("schema");

		// When
		Program result = sut.compile("benchmark 50 { ls } | schema");

		// Then
		assertThat(result.getStatements())
				.hasSize(1)
				.first().satisfies(statement -> {
					assertThat(statement.getLocation()).isEqualTo("");
					assertThat(statement.getArguments()).isEmpty();
					assertThat(statement.getCommand()).isInstanceOf(PipelineCommand.class);
					PipelineCommand pipeline = (PipelineCommand) statement.getCommand();
					assertThat(pipeline.getProducer().getCommand()).isInstanceOf(DefaultCommandDecorator.class);
					assertThat(pipeline.getConsumer().getCommand()).isSameAs(anotherCommand);
				});
	}

	@Disabled("see https://github.com/dfa1/hosh/issues/63")
	@Bug(description = "command cannot be dynamic", issue = "https://github.com/dfa1/hosh/issues/63")
	@Test
	void commandAsVariableExpansion() {
		// Given
		doReturn(Optional.of(command)).when(commandResolver).tryResolve("echo");

		// When
		Program result = sut.compile("${JAVA_HOME}/bin/java");

		// Then
		assertThat(result.getStatements()).hasSize(1)
				.first().satisfies(statement -> assertThat(statement.getArguments())
						.hasSize(0));
	}

	@Bug(description = "regression test", issue = "https://github.com/dfa1/hosh/issues/112")
	@Test
	void commentedOutCommandIsNotCompiled() {
		// Given
		doReturn(Optional.of(command)).when(commandResolver).tryResolve("cmd");

		// When
		Program result = sut.compile("cmd # ls /tmp");

		// Then
		assertThat(result.getStatements())
				.hasSize(1)
				.first().satisfies(statement -> {
					assertThat(statement.getLocation()).isEqualTo("cmd");
					assertThat(statement.getCommand()).isSameAs(command);
					assertThat(statement.getArguments()).isEmpty();
				});
	}

	@Bug(description = "regression test", issue = "https://github.com/dfa1/hosh/issues/112")
	@Test
	void commandAfterCommentBlock() {
		// Given
		doReturn(Optional.of(command)).when(commandResolver).tryResolve("cmd");

		// When
		Program result = sut.compile("#\ncmd");

		// Then
		assertThat(result.getStatements())
				.hasSize(1)
				.first().satisfies(statement -> {
					assertThat(statement.getCommand()).isSameAs(command);
					assertThat(statement.getArguments()).isEmpty();
				});
	}

	@Test
	void sequenceOfSimpleCommands() {
		// Given
		doReturn(Optional.of(command)).when(commandResolver).tryResolve("ls");
		doReturn(Optional.of(anotherCommand)).when(commandResolver).tryResolve("schema");

		// When
		Program result = sut.compile("ls /tmp; schema");

		// Then
		assertThat(result.getStatements())
				.hasSize(1)
				.first().satisfies(statement -> {
					assertThat(statement.getLocation()).isEmpty();
					assertThat(statement.getArguments()).isEmpty();
					assertThat(statement.getCommand()).isInstanceOf(SequenceCommand.class);
					SequenceCommand sequenceCommand = (SequenceCommand) statement.getCommand();
					assertThat(sequenceCommand.getFirst().getArguments()).hasSize(1);
					assertThat(sequenceCommand.getSecond().getArguments()).hasSize(0);
				});
	}

	@Test
	void sequenceOfPipelines() {
		// Given
		doReturn(Optional.of(command)).when(commandResolver).tryResolve("ls");
		doReturn(Optional.of(anotherCommand)).when(commandResolver).tryResolve("schema");

		// When
		Program result = sut.compile("ls /tmp | schema; ls /tmp | schema");

		// Then
		assertThat(result.getStatements())
				.hasSize(1)
				.first().satisfies(statement -> {
					assertThat(statement.getLocation()).isEmpty();
					assertThat(statement.getArguments()).isEmpty();
					assertThat(statement.getCommand()).isInstanceOf(SequenceCommand.class);
				});
	}

	@Test
	void lambda() {
		// Given
		doReturn(Optional.of(command)).when(commandResolver).tryResolve("ls");
		doReturn(Optional.of(anotherCommand)).when(commandResolver).tryResolve("echo");

		// When
		Program result = sut.compile("ls | { path -> echo ${path} }");

		// Then
		assertThat(result.getStatements())
				.hasSize(1)
				.first()
				.satisfies(statement -> {
					assertThat(statement.getLocation()).isEmpty();
					assertThat(statement.getArguments()).isEmpty();
					assertThat(statement.getCommand())
							.asInstanceOf(InstanceOfAssertFactories.type(PipelineCommand.class))
							.satisfies(pipelineCommand -> assertThat(pipelineCommand.getConsumer().getCommand())
									.asInstanceOf(InstanceOfAssertFactories.type(LambdaCommand.class))
									.satisfies(lambdaCommand -> assertThat(lambdaCommand.getKey()).isEqualTo("path")));
				});
	}

	/**
	 * Not sure if the following tests belong to compiler, they are more integration tests (e.g. compile + resolve).
	 */
	@Nested
	class Strings {

		@Test
		void emptySingleQuotedString() {
			// Given
			doReturn(Optional.of(command)).when(commandResolver).tryResolve("echo");

			// When
			Program result = sut.compile("echo ''");

			// Then
			assertThat(result.getStatements())
					.hasSize(1).first().satisfies(statement -> {
						assertThat(statement.getCommand()).isSameAs(command);
						assertThat(statement.getArguments()).hasSize(1).first().satisfies(argument -> {
							var resolve = argument.resolve(state);
							assertThat(resolve.asString()).isEqualTo("");
						});
					});
		}

		@Test
		void singleQuotedString() {
			// Given
			doReturn(Optional.of(command)).when(commandResolver).tryResolve("vim");

			// When
			Program result = sut.compile("vim 'file with spaces'");

			// Then
			assertThat(result.getStatements())
					.hasSize(1).first().satisfies(statement -> {
						assertThat(statement.getCommand()).isSameAs(command);
						assertThat(statement.getArguments()).hasSize(1).first().satisfies(argument -> {
							var resolve = argument.resolve(state);
							assertThat(resolve.asString()).isEqualTo("file with spaces");
						});
					});
		}

		@Test
		void singleQuotedStringWithVariables() {
			// Given
			doReturn(Optional.of(command)).when(commandResolver).tryResolve("git");

			// When
			Program result = sut.compile("git '${HOME}${BIN}'");

			// Then
			assertThat(result.getStatements())
					.hasSize(1)
					.first().satisfies(statement -> {
						assertThat(statement.getCommand()).isSameAs(command);
						assertThat(statement.getArguments()).hasSize(1).first().satisfies(argument -> {
							var resolved = argument.resolve(state);
							assertThat(resolved.asString()).isEqualTo("${HOME}${BIN}");
						});
					});
		}

		@Test
		void emptyDoubleQuotedString() {
			// Given
			doReturn(Optional.of(command)).when(commandResolver).tryResolve("vim");

			// When
			Program result = sut.compile("vim \"\"");

			// Then
			assertThat(result.getStatements())
					.hasSize(1)
					.first().satisfies(statement -> {
						assertThat(statement.getCommand()).isSameAs(command);
						assertThat(statement.getArguments()).hasSize(1).first().satisfies(argument -> {
							var resolve = argument.resolve(state);
							assertThat(resolve.asString()).isEqualTo("");
						});
					});
		}

		@Test
		void doubleQuotedString() {
			// Given
			doReturn(Optional.of(command)).when(commandResolver).tryResolve("vim");

			// When
			Program result = sut.compile("vim \"file with spaces\"");

			// Then
			assertThat(result.getStatements())
					.hasSize(1)
					.first().satisfies(statement -> {
						assertThat(statement.getCommand()).isSameAs(command);
						assertThat(statement.getArguments()).hasSize(1).first().satisfies(argument -> {
							var resolve = argument.resolve(state);
							assertThat(resolve.asString()).isEqualTo("file with spaces");
						});
					});
		}

		@Test
		void doubleQuotedStringWithVariable() {
			// Given
			doReturn(Optional.of(command)).when(commandResolver).tryResolve("ls");
			doReturn(Map.of(VariableName.constant("HOME"), Values.ofText("/home/dfa"))).when(state).getVariables();

			// When
			Program result = sut.compile("ls \"${HOME}\"");

			// Then
			assertThat(result.getStatements())
					.hasSize(1)
					.first().satisfies(statement -> {
						assertThat(statement.getCommand()).isSameAs(command);
						assertThat(statement.getArguments()).hasSize(1).first().satisfies(argument -> {
							var resolved = argument.resolve(state);
							assertThat(resolved.asString()).isEqualTo("/home/dfa");
						});
					});
		}

		@Test
		void doubleQuotedStringWithVariables() {
			// Given
			doReturn(Optional.of(command)).when(commandResolver).tryResolve("ls");
			doReturn(Map.of(VariableName.constant("HOME"), Values.ofText("/home/dfa"), VariableName.constant("BIN"), Values.ofText("bin"))).when(state).getVariables();

			// When
			Program result = sut.compile("ls \"${HOME}/${BIN}\"");

			// Then
			assertThat(result.getStatements())
					.hasSize(1)
					.first().satisfies(statement -> {
						assertThat(statement.getCommand()).isSameAs(command);
						assertThat(statement.getArguments()).hasSize(1).first().satisfies(argument -> {
							var resolved = argument.resolve(state);
							assertThat(resolved.asString()).isEqualTo("/home/dfa/bin");
						});
					});
		}

		@Test
		void doubleQuotedStringWithFallback() {
			// Given
			doReturn(Optional.of(command)).when(commandResolver).tryResolve("ls");
			doReturn(Map.of(VariableName.constant("HOME"), Values.ofText("/home/dfa"))).when(state).getVariables();

			// When
			Program result = sut.compile("ls \"${HOME!/home}/${BIN!bin}\"");

			// Then
			assertThat(result.getStatements())
					.hasSize(1)
					.first().satisfies(statement -> {
						assertThat(statement.getCommand()).isSameAs(command);
						assertThat(statement.getArguments()).hasSize(1).first().satisfies(argument -> {
							var resolved = argument.resolve(state);
							assertThat(resolved.asString()).isEqualTo("/home/dfa/bin");
						});
					});
		}
	}

}
