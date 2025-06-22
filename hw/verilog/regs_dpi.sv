import "DPI-C" context function void get_regs(input logic [33 * 32 - 1:0] regs);

module regs_dpi #(
    parameter int NR_REGS = 16
) (
    input wire clk,
    input wire resetn,
    input wire [31:0] npc,
    input wire [NR_REGS * 32 - 1:0] gprs
);
  logic [(32 - NR_REGS) * 32 - 1:0] zeros = '0;

  always_ff @(posedge clk) begin
    if (resetn) begin
      get_regs({npc, zeros, gprs});
    end
  end
endmodule
