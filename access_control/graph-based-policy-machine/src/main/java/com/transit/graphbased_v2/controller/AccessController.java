package com.transit.graphbased_v2.controller;

import com.transit.graphbased_v2.controller.dto.AccessListDTO;
import com.transit.graphbased_v2.controller.dto.AccessResponseDTO;
import com.transit.graphbased_v2.controller.dto.AccessResponseList;
import com.transit.graphbased_v2.controller.dto.OAPropertiesDTO;
import com.transit.graphbased_v2.exceptions.BadRequestException;
import com.transit.graphbased_v2.exceptions.ForbiddenException;
import com.transit.graphbased_v2.exceptions.NodeNotFoundException;
import com.transit.graphbased_v2.service.AccessService;
import com.transit.graphbased_v2.transferobjects.AccessTransferComponent;
import org.jetbrains.annotations.NotNull;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.UUID;

@RestController
@RequestMapping("/access")
public class AccessController {


    @Autowired
    private AccessService accessService;

    @NotNull
    private static AccessResponseDTO createResponse(UUID id, UUID identityId, AccessTransferComponent result) {
        OAPropertiesDTO props = new OAPropertiesDTO(result.getReadProperties(), result.getWriteProperties(), result.getShareReadProperties(), result.getShareWriteProperties());
        var responsevalue = new AccessResponseDTO(id, result.getObjectEntityClazz(), identityId, props);
        return responsevalue;
    }

    @GetMapping("/{id}")
    public ResponseEntity getAccessById(@PathVariable("id") UUID id,
                                        @RequestParam(value = "requestedById") UUID requestedById,
                                        @RequestParam(value = "identityId", required = false) UUID identityId) throws NodeNotFoundException {


        if (identityId == null) {
            identityId = requestedById;
        }
        var result = accessService.getAccess(id, identityId, requestedById);
        if (result.isEmpty()) {
            return new ResponseEntity<>(HttpStatus.NOT_FOUND);
        }

        final var responseDTO = createResponse(id, identityId, result.get());

        return new ResponseEntity<>(responseDTO, HttpStatus.OK);


    }

    @GetMapping()
    public ResponseEntity getAccessByList(@RequestParam(value = "requestedById", required = true) UUID requestedById,
                                          @RequestParam(value = "identityId", required = true) UUID identityId, @RequestBody AccessListDTO accessListDto) {


        var aList = accessService.getAccessList(accessListDto.getObjectIds(), identityId, requestedById);

        var accessResponseList = new AccessResponseList(aList.stream().map(oa -> createResponse(oa.getObjectId(), oa.getIdentityId(), oa)).toList());

        var responseEntity = new ResponseEntity((accessResponseList), HttpStatus.OK);

        return responseEntity;
    }

    @GetMapping("/search")
    public ResponseEntity getAccessByClass(
            @RequestParam(value = "requestedById") UUID requestedById,
            @RequestParam(value = "objectEntityClass") String objectEntityClass,
            @RequestParam(value = "identityId", required = false) UUID identityId,
            @RequestParam(value = "pagesize", required = false) Integer pagesize,
            @RequestParam(value = "createdByMyOwn") boolean createdByMyOwn) {


        if (pagesize == null) {
            pagesize = 300; // Default value if pagesize is not provided
        } else if (pagesize > 10000) {
            pagesize = 1000; // Limit pagesize if it exceeds 10000
        }

        return new ResponseEntity(new AccessResponseList(accessService.getAccessClazz(objectEntityClass, requestedById, createdByMyOwn, identityId, pagesize).stream().map(oa -> createResponse(oa.getObjectId(), oa.getIdentityId(), oa)).toList()), HttpStatus.OK);
    }

    @PutMapping("/{id}")
    public ResponseEntity updateRights(@PathVariable("id") UUID id,
                                       @RequestParam(value = "requestedById") UUID requestedById,
                                       @RequestParam(value = "identityId") UUID identityId,
                                       @RequestBody OAPropertiesDTO oaPropertiesdto) {


        var result = accessService.updateConnection(oaPropertiesdto.getReadProperties(),
                oaPropertiesdto.getWriteProperties(),
                oaPropertiesdto.getShareReadProperties(),
                oaPropertiesdto.getShareWriteProperties(), id, identityId, requestedById);

        if (result.isEmpty()) {
            return new ResponseEntity<>(HttpStatus.NOT_FOUND);
        }

        final var responseDTO = createResponse(id, identityId, result.get());

        return new ResponseEntity<>(responseDTO, HttpStatus.OK);
    }

    @PostMapping("/{id}")
    public ResponseEntity createRights(@PathVariable("id") UUID id,
                                       @RequestParam(value = "requestedById") UUID requestedById,
                                       @RequestParam(value = "identityId") UUID identityId,
                                       @RequestBody OAPropertiesDTO oaPropertiesdto) throws BadRequestException, ForbiddenException {

        var result = accessService.createConnection(oaPropertiesdto.getReadProperties(),
                oaPropertiesdto.getWriteProperties(),
                oaPropertiesdto.getShareReadProperties(),
                oaPropertiesdto.getShareWriteProperties(),
                id, identityId, requestedById);

        if (result.isEmpty()) {
            return new ResponseEntity<>(HttpStatus.NOT_FOUND);
        }

        final var responseDTO = createResponse(id, identityId, result.get());

        return new ResponseEntity<>(responseDTO, HttpStatus.CREATED);
    }


    @DeleteMapping("/{id}")
    public ResponseEntity deleteRights(@PathVariable("id") UUID id,
                                       @RequestParam(value = "requestedById") UUID requestedById,
                                       @RequestParam(value = "identityId") UUID identityId) {
        var x = accessService.deleteConnectionRecursive(id, identityId, requestedById);
        if (!x) {
            return new ResponseEntity(HttpStatus.BAD_REQUEST);
        }
        return new ResponseEntity(HttpStatus.NO_CONTENT);
    }


}
