module debug_dpi #(
    parameter int GPR_NUM   = 32,
    parameter int GPR_WIDTH = 32
) (
    input clk_i,
    input rst_ni,
    input [31:0] s_pc_i,
    input [31:0] s_inst_i,
    input [GprsWidth - 1:0] s_gprs_i
);
  localparam int GprsWidth = GPR_NUM * GPR_WIDTH;

  import "DPI-C" function void send_regs(input logic [GprsWidth - 1:0] regs);

  always @(posedge clk_i) begin
    if (rst_ni) begin
      send_regs(s_gprs_i);
    end
  end

endmodule
