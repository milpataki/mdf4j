/*
 * SPDX-License-Identifier: Apache-2.0
 * SPDX-FileCopyrightText: Copyright 2023 Richard Liebscher <r1tschy@posteo.de>
 */

package de.richardliebscher.mdf4;

import static java.util.Objects.requireNonNull;
import static org.assertj.core.api.Assertions.assertThat;

import de.richardliebscher.mdf4.extract.ChannelSelector;
import de.richardliebscher.mdf4.extract.RecordReader;
import de.richardliebscher.mdf4.extract.de.ObjectDeserialize;
import de.richardliebscher.mdf4.extract.de.RecordAccess;
import de.richardliebscher.mdf4.extract.de.SerializableRecordVisitor;
import de.richardliebscher.mdf4.io.ByteBufferInput;
import java.io.IOException;
import java.net.URISyntaxException;
import java.nio.ByteBuffer;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;
import java.util.stream.Stream;
import lombok.RequiredArgsConstructor;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;

public class IntegrationTest {

  @SuppressWarnings("RedundantTypeArguments")
  private static Stream<Arguments> primitive() {
    return Stream.of(
        Arguments.of("i8", List.<Byte>of((byte) 0, Byte.MAX_VALUE, Byte.MIN_VALUE)),
        Arguments.of("i16", List.<Short>of((short) 0, Short.MAX_VALUE, Short.MIN_VALUE)),
        Arguments.of("i32", List.<Integer>of(0, Integer.MAX_VALUE, Integer.MIN_VALUE)),
        Arguments.of("i64", List.<Long>of((long) 0, Long.MAX_VALUE, Long.MIN_VALUE)),
        Arguments.of("u8", List.<ObjectDeserialize.UnsignedByte>of(
            new ObjectDeserialize.UnsignedByte((byte) 0),
            new ObjectDeserialize.UnsignedByte((byte) 1),
            new ObjectDeserialize.UnsignedByte((byte) -1))),
        Arguments.of("u16", List.<ObjectDeserialize.UnsignedShort>of(
            new ObjectDeserialize.UnsignedShort((short) 0),
            new ObjectDeserialize.UnsignedShort((short) 1),
            new ObjectDeserialize.UnsignedShort((short) -1))),
        Arguments.of("u32", List.<ObjectDeserialize.UnsignedInteger>of(
            new ObjectDeserialize.UnsignedInteger(0),
            new ObjectDeserialize.UnsignedInteger(1),
            new ObjectDeserialize.UnsignedInteger(-1))),
        Arguments.of("u64", List.<ObjectDeserialize.UnsignedLong>of(
            new ObjectDeserialize.UnsignedLong(0L),
            new ObjectDeserialize.UnsignedLong(1L),
            new ObjectDeserialize.UnsignedLong(-1L))),
        Arguments.of("f32", List.<Float>of(0.f, Float.MAX_VALUE, Float.POSITIVE_INFINITY)),
        Arguments.of("f64", List.<Double>of(0.d, Double.MAX_VALUE, Double.POSITIVE_INFINITY))
    );
  }

  @ParameterizedTest
  @MethodSource("primitive")
  void checkPrimitive(String channel, List<?> expected) throws Exception {
    // ARRANGE
    final ByteBufferInput input = openMdf();
    final var mdf4File = Mdf4File.open(input);

    // ACT
    final var recordReader = mdf4File.newRecordReader(
        new SingleChannelSelector(channel), new ObjectListDeserialize());

    final var lists = collectValues(recordReader);

    // ASSERT
    assertThat(lists).containsExactlyElementsOf(expected);
  }


  @ParameterizedTest
  @MethodSource("primitive")
  void checkStreamedPrimitive(String channel, List<?> expected) throws Exception {
    // ARRANGE
    final ByteBufferInput input = openMdf();
    final var mdf4File = Mdf4File.open(input);

    // ACT
    final List<Object> lists = mdf4File.streamRecords(
            new SingleChannelSelector(channel), new ObjectListDeserialize())
            .map(rec -> rec.unwrap().get(0))
            .collect(Collectors.toList());

    // ASSERT
    assertThat(lists).containsExactlyElementsOf(expected);
  }

  private static List<Object> collectValues(RecordReader<List<Object>> recordReader)
      throws IOException {
    List<Object> values = new ArrayList<>();
    while (recordReader.remaining() != 0) {
      List<Object> record = recordReader.next();
      assertThat(record).hasSize(1);
      values.add(record.get(0));
    }
    return values;
  }

  private static ByteBufferInput openMdf() throws IOException, URISyntaxException {
    final var bytes = Files.readAllBytes(Path.of(
        requireNonNull(IntegrationTest.class.getResource("/primitives.mf4")).toURI()));
    return new ByteBufferInput(ByteBuffer.wrap(bytes));
  }

  private static class ObjectListDeserialize implements SerializableRecordVisitor<List<Object>> {

    @Override
    public List<Object> visitRecord(RecordAccess recordAccess) throws IOException {
      final var de = new ObjectDeserialize();

      final var result = new ArrayList<>();
      while (recordAccess.remaining() != 0) {
        result.add(recordAccess.nextElement(de));
      }
      return result;
    }
  }

  @RequiredArgsConstructor
  private static class SingleChannelSelector implements ChannelSelector {

    private final String signalName;

    @Override
    public boolean selectChannel(DataGroup dataGroup, ChannelGroup group, Channel channel)
        throws IOException {
      return signalName.equals(channel.getName());
    }

    @Override
    public boolean selectGroup(DataGroup dataGroup, ChannelGroup group) {
      return true;
    }
  }
}
