import 'package:flutter/material.dart';
import 'package:flutter_bloc/flutter_bloc.dart';

import '../../../../core/auth/auth_form_validators.dart';
import '../../../../core/auth/user_role.dart';
import '../cubit/login_cubit.dart';
import '../cubit/login_state.dart';

enum _AuthFormMode { login, register }

class RoleLoginPage extends StatefulWidget {
  const RoleLoginPage({super.key});

  @override
  State<RoleLoginPage> createState() => _RoleLoginPageState();
}

class _RoleLoginPageState extends State<RoleLoginPage> {
  final _loginFormKey = GlobalKey<FormState>();
  final _registerFormKey = GlobalKey<FormState>();

  final _loginIdentityController = TextEditingController();
  final _loginPasswordController = TextEditingController();

  final _registerUsernameController = TextEditingController();
  final _registerEmailController = TextEditingController();
  final _registerPhoneController = TextEditingController();
  final _registerNameController = TextEditingController();
  final _registerPasswordController = TextEditingController();
  final _registerConfirmPasswordController = TextEditingController();

  _AuthFormMode _mode = _AuthFormMode.login;
  bool _loginSubmitted = false;
  bool _registerSubmitted = false;

  @override
  void dispose() {
    _loginIdentityController.dispose();
    _loginPasswordController.dispose();
    _registerUsernameController.dispose();
    _registerEmailController.dispose();
    _registerPhoneController.dispose();
    _registerNameController.dispose();
    _registerPasswordController.dispose();
    _registerConfirmPasswordController.dispose();
    super.dispose();
  }

  @override
  Widget build(BuildContext context) {
    return BlocConsumer<LoginCubit, LoginState>(
      listenWhen: (previous, current) {
        return previous.errorMessage != current.errorMessage ||
            previous.infoMessage != current.infoMessage;
      },
      listener: (context, state) {
        final error = state.errorMessage;
        final info = state.infoMessage;
        final message = error ?? info;
        if (message == null) {
          return;
        }

        ScaffoldMessenger.of(context).showSnackBar(
          SnackBar(
            content: Text(message),
            backgroundColor:
                error == null
                    ? Theme.of(context).colorScheme.secondaryContainer
                    : Theme.of(context).colorScheme.errorContainer,
          ),
        );
        context.read<LoginCubit>().clearFeedback();
      },
      builder: (context, state) {
        final isBusy = state.isBusy;
        return Scaffold(
          body: SafeArea(
            child: Center(
              child: ConstrainedBox(
                constraints: const BoxConstraints(maxWidth: 520),
                child: SingleChildScrollView(
                  padding: const EdgeInsets.all(24),
                  child: Column(
                    crossAxisAlignment: CrossAxisAlignment.stretch,
                    children: [
                      Text(
                        'Foodya',
                        textAlign: TextAlign.center,
                        style: Theme.of(context).textTheme.displaySmall,
                      ),
                      const SizedBox(height: 8),
                      Text(
                        'Customer and Merchant access',
                        textAlign: TextAlign.center,
                        style: Theme.of(context).textTheme.bodyLarge,
                      ),
                      const SizedBox(height: 20),
                      SegmentedButton<_AuthFormMode>(
                        segments: const [
                          ButtonSegment<_AuthFormMode>(
                            value: _AuthFormMode.login,
                            icon: Icon(Icons.login),
                            label: Text('Login'),
                          ),
                          ButtonSegment<_AuthFormMode>(
                            value: _AuthFormMode.register,
                            icon: Icon(Icons.app_registration),
                            label: Text('Register'),
                          ),
                        ],
                        selected: {_mode},
                        onSelectionChanged:
                            isBusy
                                ? null
                                : (selection) {
                                  setState(() {
                                    _mode = selection.first;
                                    _loginSubmitted = false;
                                    _registerSubmitted = false;
                                  });
                                },
                      ),
                      const SizedBox(height: 20),
                      if (_mode == _AuthFormMode.login)
                        _buildLoginForm(context, state, isBusy)
                      else
                        _buildRegisterForm(context, state, isBusy),
                      const SizedBox(height: 16),
                      if (isBusy)
                        const Center(
                          child: SizedBox(
                            width: 24,
                            height: 24,
                            child: CircularProgressIndicator(strokeWidth: 2.5),
                          ),
                        ),
                    ],
                  ),
                ),
              ),
            ),
          ),
        );
      },
    );
  }

  Widget _buildLoginForm(BuildContext context, LoginState state, bool isBusy) {
    return Card(
      child: Padding(
        padding: const EdgeInsets.all(16),
        child: Form(
          key: _loginFormKey,
          autovalidateMode:
              _loginSubmitted
                  ? AutovalidateMode.onUserInteraction
                  : AutovalidateMode.disabled,
          child: Column(
            crossAxisAlignment: CrossAxisAlignment.stretch,
            children: [
              TextFormField(
                controller: _loginIdentityController,
                enabled: !isBusy,
                forceErrorText: state.fieldErrors['usernameOrEmail'],
                decoration: const InputDecoration(
                  labelText: 'Username or Email',
                  hintText: 'api_customer or api_customer@foodya.local',
                ),
                validator: AuthFormValidators.loginIdentity,
              ),
              const SizedBox(height: 12),
              TextFormField(
                controller: _loginPasswordController,
                enabled: !isBusy,
                obscureText: true,
                forceErrorText: state.fieldErrors['password'],
                decoration: const InputDecoration(
                  labelText: 'Password',
                  hintText: 'Strong@123',
                ),
                validator: AuthFormValidators.loginPassword,
              ),
              const SizedBox(height: 16),
              FilledButton.icon(
                onPressed:
                    isBusy
                        ? null
                        : () {
                          setState(() {
                            _loginSubmitted = true;
                          });
                          final isValid =
                              _loginFormKey.currentState?.validate() ?? false;
                          if (!isValid) {
                            return;
                          }

                          context.read<LoginCubit>().signIn(
                            usernameOrEmail: _loginIdentityController.text,
                            password: _loginPasswordController.text,
                          );
                        },
                icon: const Icon(Icons.login),
                label: const Text('Sign In'),
              ),
            ],
          ),
        ),
      ),
    );
  }

  Widget _buildRegisterForm(
    BuildContext context,
    LoginState state,
    bool isBusy,
  ) {
    return Card(
      child: Padding(
        padding: const EdgeInsets.all(16),
        child: Form(
          key: _registerFormKey,
          autovalidateMode:
              _registerSubmitted
                  ? AutovalidateMode.onUserInteraction
                  : AutovalidateMode.disabled,
          child: Column(
            crossAxisAlignment: CrossAxisAlignment.stretch,
            children: [
              TextFormField(
                controller: _registerUsernameController,
                enabled: !isBusy,
                forceErrorText: state.fieldErrors['username'],
                decoration: const InputDecoration(labelText: 'Username'),
                validator: AuthFormValidators.requiredField,
              ),
              const SizedBox(height: 12),
              TextFormField(
                controller: _registerEmailController,
                enabled: !isBusy,
                keyboardType: TextInputType.emailAddress,
                forceErrorText: state.fieldErrors['email'],
                decoration: const InputDecoration(labelText: 'Email'),
                validator: AuthFormValidators.email,
              ),
              const SizedBox(height: 12),
              TextFormField(
                controller: _registerPhoneController,
                enabled: !isBusy,
                keyboardType: TextInputType.phone,
                forceErrorText: state.fieldErrors['phoneNumber'],
                decoration: const InputDecoration(labelText: 'Phone Number'),
                validator: AuthFormValidators.phoneNumber,
              ),
              const SizedBox(height: 12),
              TextFormField(
                controller: _registerNameController,
                enabled: !isBusy,
                forceErrorText: state.fieldErrors['fullName'],
                decoration: const InputDecoration(labelText: 'Full Name'),
                validator: AuthFormValidators.requiredField,
              ),
              const SizedBox(height: 12),
              TextFormField(
                controller: _registerPasswordController,
                enabled: !isBusy,
                obscureText: true,
                forceErrorText: state.fieldErrors['password'],
                decoration: const InputDecoration(labelText: 'Password'),
                validator: AuthFormValidators.password,
              ),
              const SizedBox(height: 12),
              TextFormField(
                controller: _registerConfirmPasswordController,
                enabled: !isBusy,
                obscureText: true,
                forceErrorText: state.fieldErrors['confirmPassword'],
                decoration: const InputDecoration(
                  labelText: 'Confirm Password',
                ),
                validator:
                    (value) => AuthFormValidators.confirmPassword(
                      password: _registerPasswordController.text,
                      confirmPassword: value,
                    ),
              ),
              const SizedBox(height: 12),
              DropdownButtonFormField<UserRole>(
                value: state.registrationRole,
                decoration: InputDecoration(
                  labelText: 'Role',
                  errorText: state.fieldErrors['role'],
                ),
                items:
                    UserRole.values
                        .map(
                          (role) => DropdownMenuItem<UserRole>(
                            value: role,
                            child: Text(role.label),
                          ),
                        )
                        .toList(),
                onChanged:
                    isBusy
                        ? null
                        : (role) {
                          if (role != null) {
                            context.read<LoginCubit>().setRegistrationRole(
                              role,
                            );
                          }
                        },
              ),
              const SizedBox(height: 16),
              FilledButton.icon(
                onPressed:
                    isBusy
                        ? null
                        : () {
                          setState(() {
                            _registerSubmitted = true;
                          });
                          final isValid =
                              _registerFormKey.currentState?.validate() ??
                              false;
                          if (!isValid) {
                            return;
                          }

                          context.read<LoginCubit>().register(
                            username: _registerUsernameController.text,
                            email: _registerEmailController.text,
                            phoneNumber: _registerPhoneController.text,
                            fullName: _registerNameController.text,
                            password: _registerPasswordController.text,
                            role: state.registrationRole,
                          );
                        },
                icon: const Icon(Icons.app_registration),
                label: const Text('Create Account'),
              ),
            ],
          ),
        ),
      ),
    );
  }
}
