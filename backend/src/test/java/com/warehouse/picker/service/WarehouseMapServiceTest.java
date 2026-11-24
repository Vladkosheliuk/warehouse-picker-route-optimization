package com.warehouse.picker.service;

import com.warehouse.picker.dto.WarehouseMapDTO;
import com.warehouse.picker.dto.WarehouseMapDTO.EdgeDTO;
import com.warehouse.picker.dto.WarehouseMapDTO.NodeDTO;
import com.warehouse.picker.dto.WarehouseMapDTO.NodeType;
import com.warehouse.picker.entity.Warehouse;
import com.warehouse.picker.entity.WarehouseEdge;
import com.warehouse.picker.entity.WarehouseNode;
import com.warehouse.picker.repository.WarehouseEdgeRepository;
import com.warehouse.picker.repository.WarehouseNodeRepository;
import com.warehouse.picker.repository.WarehouseRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.math.BigDecimal;
import java.util.List;
import java.util.NoSuchElementException;
import java.util.Optional;
import java.util.concurrent.atomic.AtomicReference;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyList;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class WarehouseMapServiceTest {

    @Mock
    private WarehouseRepository warehouseRepository;

    @Mock
    private WarehouseNodeRepository nodeRepository;

    @Mock
    private WarehouseEdgeRepository edgeRepository;

    @InjectMocks
    private WarehouseMapService service;

    private Warehouse warehouse;

    @BeforeEach
    void setUp() {
        warehouse = new Warehouse();
        warehouse.setName("Test Warehouse");
    }

    @Test
    void shouldImportValidMap() {
        givenEmptyWarehouse();

        WarehouseMapDTO input = validMap();

        AtomicReference<List<WarehouseNode>> savedNodes =
                new AtomicReference<>();

        AtomicReference<List<WarehouseEdge>> savedEdges =
                new AtomicReference<>();

        doAnswer(invocation -> {
            savedNodes.set(invocation.getArgument(0));
            return invocation.getArgument(0);
        }).when(nodeRepository).saveAllAndFlush(anyList());

        doAnswer(invocation -> {
            savedEdges.set(invocation.getArgument(0));
            return invocation.getArgument(0);
        }).when(edgeRepository).saveAllAndFlush(anyList());

        WarehouseMapDTO result = service.importMap(1L, input);

        assertSame(input, result);

        assertNotNull(savedNodes.get());
        assertEquals(3, savedNodes.get().size());

        assertEquals(
                List.of("E", "S", "X"),
                savedNodes.get().stream()
                        .map(WarehouseNode::getNodeCode)
                        .toList()
        );

        assertTrue(
                savedNodes.get().stream()
                        .allMatch(node ->
                                node.getWarehouse() == warehouse)
        );

        assertNotNull(savedEdges.get());
        assertEquals(2, savedEdges.get().size());

        WarehouseEdge first = savedEdges.get().get(0);

        assertEquals("E", first.getFromNode().getNodeCode());
        assertEquals("S", first.getToNode().getNodeCode());
        assertEquals(0, first.getDistanceM()
                .compareTo(BigDecimal.TEN));
        assertFalse(first.isBidirectional());

        assertSame(warehouse, first.getWarehouse());

        verify(nodeRepository).saveAllAndFlush(anyList());
        verify(edgeRepository).saveAllAndFlush(anyList());
    }

    @Test
    void shouldImportBidirectionalMap() {
        givenEmptyWarehouse();

        WarehouseMapDTO input = map(
                standardNodes(),
                edge("E", "S", true),
                edge("S", "X", true)
        );

        assertSame(input, service.importMap(1L, input));

        verify(nodeRepository).saveAllAndFlush(anyList());
        verify(edgeRepository).saveAllAndFlush(anyList());
    }

    @Test
    void shouldAcceptReverseDirectedEdge() {
        givenEmptyWarehouse();

        WarehouseMapDTO input = map(
                standardNodes(),
                edge("E", "S", false),
                edge("S", "E", false),
                edge("S", "X", false)
        );

        assertDoesNotThrow(
                () -> service.importMap(1L, input)
        );
    }

    @Test
    void shouldRejectUnknownWarehouseOnImport() {
        when(warehouseRepository.findByIdForUpdate(999L))
                .thenReturn(Optional.empty());

        NoSuchElementException exception = assertThrows(
                NoSuchElementException.class,
                () -> service.importMap(999L, validMap())
        );

        assertEquals(
                "Warehouse not found: 999",
                exception.getMessage()
        );

        verifyNoInteractions(nodeRepository, edgeRepository);
    }

    @Test
    void shouldRejectImportWhenNodesAlreadyExist() {
        givenWarehouse();

        WarehouseNode existing = new WarehouseNode();

        when(nodeRepository.findByWarehouseId(1L))
                .thenReturn(List.of(existing));

        IllegalStateException exception = assertThrows(
                IllegalStateException.class,
                () -> service.importMap(1L, validMap())
        );

        assertTrue(exception.getMessage()
                .contains("already contains a map"));

        verify(nodeRepository, never())
                .saveAllAndFlush(any());
        verify(edgeRepository, never())
                .saveAllAndFlush(any());
    }

    @Test
    void shouldRejectImportWhenEdgesAlreadyExist() {
        givenWarehouse();

        when(nodeRepository.findByWarehouseId(1L))
                .thenReturn(List.of());

        when(edgeRepository.findByWarehouseId(1L))
                .thenReturn(List.of(new WarehouseEdge()));

        assertThrows(
                IllegalStateException.class,
                () -> service.importMap(1L, validMap())
        );

        verify(nodeRepository, never())
                .saveAllAndFlush(any());
        verify(edgeRepository, never())
                .saveAllAndFlush(any());
    }

    @Test
    void shouldRejectDuplicateNodeCodes() {
        givenEmptyWarehouse();

        WarehouseMapDTO input = map(
                List.of(
                        node("E", NodeType.ENTRANCE),
                        node("S", NodeType.STORAGE),
                        node("S", NodeType.INTERSECTION),
                        node("X", NodeType.EXIT)
                ),
                edge("E", "S", false),
                edge("S", "X", false)
        );

        IllegalArgumentException exception =
                assertInvalidMap(input);

        assertTrue(exception.getMessage()
                .contains("Duplicate node code"));
    }

    @Test
    void shouldRejectMapWithoutEntrance() {
        givenEmptyWarehouse();

        WarehouseMapDTO input = map(
                List.of(
                        node("I", NodeType.INTERSECTION),
                        node("S", NodeType.STORAGE),
                        node("X", NodeType.EXIT)
                ),
                edge("I", "S", false),
                edge("S", "X", false)
        );

        IllegalArgumentException exception =
                assertInvalidMap(input);

        assertTrue(exception.getMessage()
                .contains("exactly one ENTRANCE"));
    }

    @Test
    void shouldRejectMapWithoutExit() {
        givenEmptyWarehouse();

        WarehouseMapDTO input = map(
                List.of(
                        node("E", NodeType.ENTRANCE),
                        node("S", NodeType.STORAGE),
                        node("I", NodeType.INTERSECTION)
                ),
                edge("E", "S", false),
                edge("S", "I", false)
        );

        IllegalArgumentException exception =
                assertInvalidMap(input);

        assertTrue(exception.getMessage()
                .contains("exactly one EXIT"));
    }

    @Test
    void shouldRejectMultipleEntrances() {
        givenEmptyWarehouse();

        WarehouseMapDTO input = map(
                List.of(
                        node("E1", NodeType.ENTRANCE),
                        node("E2", NodeType.ENTRANCE),
                        node("S", NodeType.STORAGE),
                        node("X", NodeType.EXIT)
                ),
                edge("E1", "S", false),
                edge("S", "X", false)
        );

        assertInvalidMap(input);
    }

    @Test
    void shouldRejectMapWithoutStorage() {
        givenEmptyWarehouse();

        WarehouseMapDTO input = map(
                List.of(
                        node("E", NodeType.ENTRANCE),
                        node("I", NodeType.INTERSECTION),
                        node("X", NodeType.EXIT)
                ),
                edge("E", "I", false),
                edge("I", "X", false)
        );

        IllegalArgumentException exception =
                assertInvalidMap(input);

        assertTrue(exception.getMessage()
                .contains("at least one STORAGE"));
    }

    @Test
    void shouldRejectEdgeReferencingUnknownNode() {
        givenEmptyWarehouse();

        WarehouseMapDTO input = map(
                standardNodes(),
                edge("E", "UNKNOWN", false),
                edge("S", "X", false)
        );

        IllegalArgumentException exception =
                assertInvalidMap(input);

        assertTrue(exception.getMessage()
                .contains("unknown node"));
    }

    @Test
    void shouldRejectSelfLoop() {
        givenEmptyWarehouse();

        WarehouseMapDTO input = map(
                standardNodes(),
                edge("E", "E", false),
                edge("E", "S", false),
                edge("S", "X", false)
        );

        IllegalArgumentException exception =
                assertInvalidMap(input);

        assertTrue(exception.getMessage()
                .contains("cannot connect a node to itself"));
    }

    @Test
    void shouldRejectDuplicateEdges() {
        givenEmptyWarehouse();

        WarehouseMapDTO input = map(
                standardNodes(),
                edge("E", "S", false),
                edge("E", "S", false),
                edge("S", "X", false)
        );

        IllegalArgumentException exception =
                assertInvalidMap(input);

        assertTrue(exception.getMessage()
                .contains("Duplicate edge"));
    }

    @Test
    void shouldRejectStorageUnreachableFromEntrance() {
        givenEmptyWarehouse();

        WarehouseMapDTO input = map(
                standardNodes(),
                edge("E", "X", false),
                edge("S", "X", false)
        );

        IllegalArgumentException exception =
                assertInvalidMap(input);

        assertTrue(exception.getMessage()
                .contains("Every node must be reachable"));
    }

    @Test
    void shouldRejectNodeWithoutPathToExit() {
        givenEmptyWarehouse();

        WarehouseMapDTO input = map(
                standardNodes(),
                edge("E", "S", false),
                edge("E", "X", false)
        );

        IllegalArgumentException exception =
                assertInvalidMap(input);

        assertTrue(exception.getMessage()
                .contains("path to the exit"));
    }

    @Test
    void shouldRejectUnknownWarehouseOnRead() {
        when(warehouseRepository.existsById(999L))
                .thenReturn(false);

        NoSuchElementException exception = assertThrows(
                NoSuchElementException.class,
                () -> service.getMap(999L)
        );

        assertEquals(
                "Warehouse not found: 999",
                exception.getMessage()
        );
    }

    @Test
    void shouldReturnEmptyMapForExistingWarehouse() {
        when(warehouseRepository.existsById(1L))
                .thenReturn(true);

        when(nodeRepository.findByWarehouseId(1L))
                .thenReturn(List.of());

        when(edgeRepository.findByWarehouseId(1L))
                .thenReturn(List.of());

        WarehouseMapDTO result = service.getMap(1L);

        assertTrue(result.nodes().isEmpty());
        assertTrue(result.edges().isEmpty());
    }

    @Test
    void shouldReturnExistingWarehouseMap() {
        when(warehouseRepository.existsById(1L))
                .thenReturn(true);

        WarehouseNode entrance = new WarehouseNode();
        entrance.setWarehouse(warehouse);
        entrance.setNodeCode("E");
        entrance.setNodeType("ENTRANCE");
        entrance.setXCoordinate(BigDecimal.ZERO);
        entrance.setYCoordinate(BigDecimal.ZERO);

        WarehouseNode storage = new WarehouseNode();
        storage.setWarehouse(warehouse);
        storage.setNodeCode("S");
        storage.setNodeType("STORAGE");
        storage.setXCoordinate(new BigDecimal("10.00"));
        storage.setYCoordinate(new BigDecimal("5.00"));

        WarehouseEdge connection = new WarehouseEdge();
        connection.setWarehouse(warehouse);
        connection.setFromNode(entrance);
        connection.setToNode(storage);
        connection.setDistanceM(new BigDecimal("12.50"));
        connection.setBidirectional(true);

        when(nodeRepository.findByWarehouseId(1L))
                .thenReturn(List.of(entrance, storage));

        when(edgeRepository.findByWarehouseId(1L))
                .thenReturn(List.of(connection));

        WarehouseMapDTO result = service.getMap(1L);

        assertEquals(2, result.nodes().size());
        assertEquals(1, result.edges().size());

        assertEquals("E", result.nodes().get(0).code());
        assertEquals(
                NodeType.ENTRANCE,
                result.nodes().get(0).type()
        );

        assertEquals("S", result.nodes().get(1).code());
        assertEquals(
                0,
                result.nodes().get(1).x()
                        .compareTo(new BigDecimal("10.00"))
        );

        EdgeDTO edge = result.edges().get(0);

        assertEquals("E", edge.from());
        assertEquals("S", edge.to());
        assertEquals(
                0,
                edge.distanceM()
                        .compareTo(new BigDecimal("12.50"))
        );
        assertTrue(edge.bidirectional());
    }

    private IllegalArgumentException assertInvalidMap(
            WarehouseMapDTO input
    ) {
        IllegalArgumentException exception = assertThrows(
                IllegalArgumentException.class,
                () -> service.importMap(1L, input)
        );

        verify(nodeRepository, never())
                .saveAllAndFlush(any());

        verify(edgeRepository, never())
                .saveAllAndFlush(any());

        return exception;
    }

    private void givenWarehouse() {
        when(warehouseRepository.findByIdForUpdate(1L))
                .thenReturn(Optional.of(warehouse));
    }

    private void givenEmptyWarehouse() {
        givenWarehouse();

        when(nodeRepository.findByWarehouseId(1L))
                .thenReturn(List.of());

        when(edgeRepository.findByWarehouseId(1L))
                .thenReturn(List.of());
    }

    private static WarehouseMapDTO validMap() {
        return map(
                standardNodes(),
                edge("E", "S", false),
                edge("S", "X", false)
        );
    }

    private static List<NodeDTO> standardNodes() {
        return List.of(
                node("E", NodeType.ENTRANCE),
                node("S", NodeType.STORAGE),
                node("X", NodeType.EXIT)
        );
    }

    private static NodeDTO node(
            String code,
            NodeType type
    ) {
        return new NodeDTO(
                code,
                type,
                BigDecimal.ZERO,
                BigDecimal.ZERO
        );
    }

    private static EdgeDTO edge(
            String from,
            String to,
            boolean bidirectional
    ) {
        return new EdgeDTO(
                from,
                to,
                BigDecimal.TEN,
                bidirectional
        );
    }

    private static WarehouseMapDTO map(
            List<NodeDTO> nodes,
            EdgeDTO... edges
    ) {
        return new WarehouseMapDTO(
                nodes,
                List.of(edges)
        );
    }
}
