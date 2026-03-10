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

  import "DPI-C" function void send_state(
    input bit [31:0] pc,
    input bit [31:0] inst,
    input bit [GprsWidth - 1:0] gprs
  );

  always_comb begin
    if (rst_ni) begin
      send_state(s_pc_i, s_inst_i, s_gprs_i);
    end
  end

endmodule
