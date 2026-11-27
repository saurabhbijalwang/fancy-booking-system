package com.example.auction.api;

import jakarta.validation.Valid;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotNull;

import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import com.example.auction.api.ApiModels.AuctionReport;
import com.example.auction.api.ApiModels.AuctionResponse;
import com.example.auction.api.ApiModels.BidResponse;
import com.example.auction.api.ApiModels.CreateAuctionRequest;
import com.example.auction.api.ApiModels.PlaceBidRequest;
import com.example.auction.service.AuctionService;

@RestController
@RequestMapping("/api/auctions")
public class AuctionController {
    private final AuctionService auctionService;

    public AuctionController(AuctionService auctionService) {
        this.auctionService = auctionService;
    }

    @PostMapping
    public ResponseEntity<AuctionResponse> createAuction(@Valid @RequestBody CreateAuctionRequest request) {
        AuctionResponse response = auctionService.createAuction(
                request.title(), request.durationMinutes(), request.startingBid());
        return ResponseEntity.status(201).body(response);
    }

    @GetMapping("/{auctionId}")
    public AuctionResponse getAuction(@PathVariable long auctionId) {
        return auctionService.getAuction(auctionId);
    }

    @PostMapping("/{auctionId}/bids")
    public ResponseEntity<BidResponse> placeBid(
            @PathVariable long auctionId,
            @Valid @RequestBody PlaceBidRequest request) {
        return ResponseEntity.status(201).body(auctionService.placeBid(auctionId, request.userId()));
    }

    @GetMapping("/{auctionId}/report")
    public AuctionReport getReport(@PathVariable long auctionId) {
        return auctionService.getReport(auctionId);
    }

    @GetMapping(value = "/{auctionId}/report.csv", produces = "text/csv")
    public ResponseEntity<String> downloadCsv(@PathVariable long auctionId) {
        AuctionReport report = auctionService.getReport(auctionId);
        StringBuilder csv = new StringBuilder("auction_number,bid_sequence,user_id,user_name,amount,timestamp\n");
        for (var bid : report.bids()) {
            csv.append(report.auctionNumber()).append(',')
                    .append(bid.sequence()).append(',')
                    .append(bid.userId()).append(',')
                    .append(csvCell(bid.userName())).append(',')
                    .append(bid.amount()).append(',')
                    .append(bid.timestamp()).append('\n');
        }
        return ResponseEntity.ok()
                .header(HttpHeaders.CONTENT_DISPOSITION,
                        "attachment; filename=\"auction-" + report.auctionNumber() + "-report.csv\"")
                .contentType(MediaType.parseMediaType("text/csv"))
                .body(csv.toString());
    }

    private String csvCell(String value) {
        return "\"" + value.replace("\"", "\"\"") + "\"";
    }
}
