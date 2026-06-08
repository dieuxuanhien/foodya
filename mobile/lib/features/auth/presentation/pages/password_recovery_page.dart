import 'package:flutter/material.dart';
import 'package:flutter_bloc/flutter_bloc.dart';
import 'package:go_router/go_router.dart';

import '../../../../core/auth/auth_form_validators.dart';
import '../../../../core/network/api_error_ui_message.dart';
import '../../domain/repositories/auth_repository.dart';

class PasswordRecoveryPage extends StatefulWidget {
  const PasswordRecoveryPage({super.key});

  @override
  State<PasswordRecoveryPage> createState() => _PasswordRecoveryPageState();
}

class _PasswordRecoveryPageState extends State<PasswordRecoveryPage> {
  final _emailController = TextEditingController();
  final _otpController = TextEditingController();
  final _passwordController = TextEditingController();
  final _confirmController = TextEditingController();

  String? _challengeToken;
  String? _resetToken;
  String? _deliveryHint;
  bool _busy = false;

  @override
  void dispose() {
    _emailController.dispose();
    _otpController.dispose();
    _passwordController.dispose();
    _confirmController.dispose();
    super.dispose();
  }

  @override
  Widget build(BuildContext context) {
    return Scaffold(
      appBar: AppBar(title: const Text('Reset Password')),
      body: ListView(
        padding: const EdgeInsets.all(16),
        children: [
          TextField(
            controller: _emailController,
            keyboardType: TextInputType.emailAddress,
            decoration: const InputDecoration(labelText: 'Email'),
          ),
          const SizedBox(height: 12),
          FilledButton(
            onPressed: _busy ? null : _requestOtp,
            child: const Text('Send OTP'),
          ),
          if (_deliveryHint != null) ...[
            const SizedBox(height: 12),
            Text('OTP sent to $_deliveryHint'),
          ],
          const SizedBox(height: 20),
          TextField(
            controller: _otpController,
            keyboardType: TextInputType.number,
            decoration: const InputDecoration(labelText: 'OTP'),
            maxLength: 6,
          ),
          FilledButton.tonal(
            onPressed: _busy || _challengeToken == null ? null : _verifyOtp,
            child: const Text('Verify OTP'),
          ),
          const SizedBox(height: 20),
          TextField(
            controller: _passwordController,
            obscureText: true,
            decoration: const InputDecoration(labelText: 'New password'),
          ),
          const SizedBox(height: 12),
          TextField(
            controller: _confirmController,
            obscureText: true,
            decoration: const InputDecoration(labelText: 'Confirm password'),
          ),
          const SizedBox(height: 12),
          FilledButton.icon(
            onPressed: _busy || _resetToken == null ? null : _resetPassword,
            icon:
                _busy
                    ? const SizedBox(
                      width: 18,
                      height: 18,
                      child: CircularProgressIndicator(strokeWidth: 2),
                    )
                    : const Icon(Icons.lock_reset_outlined),
            label: const Text('Reset password'),
          ),
        ],
      ),
    );
  }

  Future<void> _requestOtp() async {
    final error = AuthFormValidators.email(_emailController.text);
    if (error != null) {
      _show(error);
      return;
    }
    final repository = context.read<AuthRepository>();
    await _run(() async {
      final result = await repository.forgotPassword(_emailController.text);
      if (!mounted) {
        return;
      }
      setState(() {
        _challengeToken = result.challengeToken;
        _deliveryHint = result.deliveryHint;
      });
    });
  }

  Future<void> _verifyOtp() async {
    final repository = context.read<AuthRepository>();
    await _run(() async {
      final result = await repository.verifyOtp(
        challengeToken: _challengeToken!,
        otp: _otpController.text,
      );
      if (!mounted) {
        return;
      }
      setState(() => _resetToken = result.resetToken);
    });
  }

  Future<void> _resetPassword() async {
    final repository = context.read<AuthRepository>();
    await _run(() async {
      await repository.resetPassword(
        resetToken: _resetToken!,
        newPassword: _passwordController.text,
        confirmPassword: _confirmController.text,
      );
      if (!mounted) {
        return;
      }
      _show('Password reset. Sign in with your new password.');
      context.go('/login');
    });
  }

  Future<void> _run(Future<void> Function() action) async {
    if (!mounted) {
      return;
    }
    setState(() => _busy = true);
    try {
      await action();
    } catch (error) {
      if (!mounted) {
        return;
      }
      final presentation = ApiErrorUiMessageMapper.mapAny(
        error,
        fallback: 'Password recovery failed.',
      );
      _show(presentation.message);
    } finally {
      if (mounted) {
        setState(() => _busy = false);
      }
    }
  }

  void _show(String message) {
    if (!mounted) {
      return;
    }
    ScaffoldMessenger.of(
      context,
    ).showSnackBar(SnackBar(content: Text(message)));
  }
}
