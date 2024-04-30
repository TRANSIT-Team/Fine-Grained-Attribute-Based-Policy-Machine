package com.transit.graphbased_v2.controller;

import com.transit.graphbased_v2.controller.dto.AccessResponseDTO;
import com.transit.graphbased_v2.controller.dto.EntityPropertiesDTO;
import com.transit.graphbased_v2.controller.dto.OAPropertiesDTO;
import com.transit.graphbased_v2.controller.dto.ObjectDTO;
import com.transit.graphbased_v2.domain.graph.nodes.ClazzType;
import com.transit.graphbased_v2.domain.graph.nodes.IdentityClazz;
import com.transit.graphbased_v2.exceptions.BadRequestException;
import com.transit.graphbased_v2.repository.AssigmentRepository;
import com.transit.graphbased_v2.repository.ObjectClazzRepository;
import com.transit.graphbased_v2.service.AccessService;
import com.transit.graphbased_v2.service.HelperService;
import com.transit.graphbased_v2.service.IdentityService;
import com.transit.graphbased_v2.service.ObjectService;
import com.transit.graphbased_v2.transferobjects.AccessTransferComponent;
import org.jetbrains.annotations.NotNull;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.HashMap;
import java.util.Map;
import java.util.UUID;


@RestController
@RequestMapping("/helpers")
public class HelpersController {
    @Autowired
    private ObjectClazzRepository objectClazzRepository;

    @Autowired
    private AssigmentRepository assigmentRepository;

    @Autowired
    private HelperService helperService;


    @Autowired
    private AccessService accessService;


    @Autowired
    private ObjectService objectService;


    @Autowired
    private IdentityService identityService;

    @NotNull
    private static AccessResponseDTO createResponse(UUID id, UUID identityId, AccessTransferComponent result) {
        OAPropertiesDTO props = new OAPropertiesDTO(result.getReadProperties(), result.getWriteProperties(), result.getShareReadProperties(), result.getShareWriteProperties());
        var responsevalue = new AccessResponseDTO(id, result.getObjectEntityClazz(), identityId, props);
        return responsevalue;
    }

    @GetMapping("/isshared/{id}")
    public ResponseEntity getIsShared(@PathVariable("id") UUID id) {
        Boolean response = false;
        var node = objectClazzRepository.findById(id);
        if (node.isPresent()) {
            var outgoingEdges = assigmentRepository.getOutgoingRelationships(id);
            if (outgoingEdges.size() > 1) {
                response = true;
            }
        }
        return new ResponseEntity<>(response, HttpStatus.OK);
    }

    @GetMapping("/entityclass/{id}")
    public ResponseEntity getEntityClass(@PathVariable("id") UUID id) {
        String response = null;
        var node = objectClazzRepository.findById(id);
        if (node.isPresent()) {
            response = node.get().getEntityClass();
        } else {
            return new ResponseEntity(HttpStatus.NOT_FOUND);
        }
        return new ResponseEntity<>(response, HttpStatus.OK);
    }

    @PostMapping("/entity/renameProperty")
    public ResponseEntity<Object> renameEntityProperty(@RequestParam(value = "requestedById") UUID requestedById, @RequestBody EntityPropertiesDTO requestDTO) throws BadRequestException {

        var x = helperService.renamePropertyOfEntity(requestedById, requestDTO);
        Map<String, String> response = new HashMap<>();
        response.put("message", "Entityproperty '" + requestDTO.getPropertyOldName() + "' renamed to '" + requestDTO.getPropertyNewName() + "' for all objects of entity '" + requestDTO.getEntityClass() + "'.");

        return new ResponseEntity<>(response, HttpStatus.OK);

    }

    @PostMapping("/entity/addProperty")
    public ResponseEntity<Object> addEntityProperty(@RequestParam(value = "requestedById") UUID requestedById, @RequestBody EntityPropertiesDTO requestDTO) throws BadRequestException {

        var x = helperService.addPropertyOfEntity(requestedById, requestDTO);
        Map<String, String> response = new HashMap<>();
        response.put("message", "Entityproperty '" + requestDTO.getPropertyNewName() + "' added to all objects of entity '" + requestDTO.getEntityClass() + "'.");

        return new ResponseEntity<>(response, HttpStatus.OK);

    }

    @PostMapping("/object/access")
    public ResponseEntity<Object> createIdentitiesaddObjectAndGiveAccess(@RequestParam(value = "identityId") UUID identityId, @RequestParam(value = "requestedById") UUID requestedById, @RequestBody ObjectDTO requestDTO) throws BadRequestException {


        var opt = identityService.getIdentity(identityId);

        if (opt.isEmpty()) {
            IdentityClazz nodeDTO = new IdentityClazz();
            nodeDTO.setId(identityId);
            nodeDTO.setName("identity#" + identityId);
            nodeDTO.setType(ClazzType.I);
            nodeDTO.setEntityClass(null);

            IdentityClazz createdNodeDTO = identityService.createIdentity(nodeDTO);
        }

        var opt2 = identityService.getIdentity(requestedById);

        if (opt2.isEmpty()) {
            IdentityClazz nodeDTO2 = new IdentityClazz();
            nodeDTO2.setId(requestedById);
            nodeDTO2.setName("identity#" + requestedById);
            nodeDTO2.setType(ClazzType.I);
            nodeDTO2.setEntityClass(null);

            IdentityClazz createdNodeDTO2 = identityService.createIdentity(nodeDTO2);
        }

        requestDTO.setIdentityId(requestedById);

        var response = objectService.createObject(requestDTO);
        var id = response.getObjectId();


        var result = accessService.createConnection(requestDTO.getProperties(), requestDTO.getProperties(), requestDTO.getProperties(), requestDTO.getProperties(), id, identityId, requestedById);

        if (result.isEmpty()) {
            return new ResponseEntity<>(HttpStatus.NOT_FOUND);
        }

        final var responseDTO = createResponse(id, identityId, result.get());

        return new ResponseEntity<>(responseDTO, HttpStatus.CREATED);
    }

}
