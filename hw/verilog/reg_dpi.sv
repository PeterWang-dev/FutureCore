import "DPI-C" function void get_regs(
    input bit[32 * 32 - 1:0] gprs
);

module reg16_dpi (
    input wire clk,
    input wire resetn,
    input wire [16 * 32 - 1:0] gprs
);

bit [32 * 32 - 1:0] zz_gprs;

assign zz_gprs[16 * 32 - 1:0] = gprs; // Assign first 16 registers
genvar i;
generate
    for (i = 16; i < 32; i++) begin : gen_zero_out_unused_gprs
        assign zz_gprs[32 * (i + 1) - 1:32 * i] = 0; // Zero out unused registers
    end
endgenerate

always_ff @(posedge clk) begin
    if (resetn) begin
        get_regs(zz_gprs);
    end
end
endmodule

module reg32_dpi (
    input wire clk,
    input wire resetn,
    input wire [32 * 32 - 1:0] gprs
);
always_ff @(posedge clk) begin
    if (resetn) begin
        get_regs(gprs);
    end
end
endmodule